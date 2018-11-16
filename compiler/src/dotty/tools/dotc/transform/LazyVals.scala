package dotty.tools.dotc
package transform

import dotty.tools.dotc.core.Annotations.Annotation

import scala.collection.mutable
import core._
import Contexts._
import Symbols._
import Decorators._
import NameKinds._
import Types._
import Flags.FlagSet
import StdNames.nme
import dotty.tools.dotc.transform.MegaPhase._
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Types.MethodType
import SymUtils._
import dotty.tools.dotc.core.DenotTransformers.IdentityDenotTransformer
import Erasure.Boxing.adaptToType

import java.util.IdentityHashMap

class LazyVals extends MiniPhase with IdentityDenotTransformer {
  import LazyVals._
  import tpd._

  /** this map contains mutable state of transformation: OffsetDefs to be appended to companion object definitions,
    * and number of bits currently used */
  class OffsetInfo(var defs: List[Tree], var ord:Int)
  private[this] val appendOffsetDefs = mutable.Map.empty[Symbol, OffsetInfo]

  override def phaseName: String = "lazyVals"

  /** List of names of phases that should have finished processing of tree
    * before this phase starts processing same tree */
  override def runsAfter: Set[String] = Set(Mixin.name, CollectNullableFields.name)

  override def changesMembers: Boolean = true  // the phase adds lazy val accessors

  def transformer: LazyVals = new LazyVals

  val containerFlags: FlagSet = Flags.Synthetic | Flags.Mutable | Flags.Lazy
  val initFlags: FlagSet      = Flags.Synthetic | Flags.Method

  val containerFlagsMask: FlagSet = Flags.Method | Flags.Lazy | Flags.Accessor | Flags.Module

  /** A map of lazy values to the fields they should null after initialization. */
  private[this] var lazyValNullables: IdentityHashMap[Symbol, mutable.ListBuffer[Symbol]] = _
  private def nullableFor(sym: Symbol)(implicit ctx: Context) = {
    // optimisation: value only used once, we can remove the value from the map
    val nullables = lazyValNullables.remove(sym)
    if (nullables == null) Nil
    else nullables.toList
  }


  override def prepareForUnit(tree: Tree)(implicit ctx: Context): Context = {
    if (lazyValNullables == null)
      lazyValNullables = ctx.base.collectNullableFieldsPhase.asInstanceOf[CollectNullableFields].lazyValNullables
    ctx
  }

  override def transformDefDef(tree: DefDef)(implicit ctx: Context): Tree =
   transformLazyVal(tree)


  override def transformValDef(tree: ValDef)(implicit ctx: Context): Tree = {
    transformLazyVal(tree)
  }

  def transformLazyVal(tree: ValOrDefDef)(implicit ctx: Context): Tree = {
    val sym = tree.symbol
    if (!(sym is Flags.Lazy) ||
        sym.owner.is(Flags.Trait) || // val is accessor, lazy field will be implemented in subclass
        (sym.isStatic && sym.is(Flags.Module, butNot = Flags.Method))) // static module vals are implemented in the JVM by lazy loading
      tree
    else {
      val isField = sym.owner.isClass
      if (isField) {
        if (sym.isVolatile ||
           (sym.is(Flags.Module)/* || ctx.scala2Mode*/) &&
            // TODO assume @volatile once LazyVals uses static helper constructs instead of
            // ones in the companion object.
           !sym.is(Flags.Synthetic))
            // module class is user-defined.
            // Should be threadsafe, to mimic safety guaranteed by global object
          transformMemberDefVolatile(tree)
        else if (sym.is(Flags.Module)) // synthetic module
          transformSyntheticModule(tree)
        else
          transformMemberDefNonVolatile(tree)
      }
      else transformLocalDef(tree)
    }
  }


  /** Append offset fields to companion objects
    */
  override def transformTemplate(template: Template)(implicit ctx: Context): Tree = {
    val cls = ctx.owner.asClass

    appendOffsetDefs.get(cls) match {
      case None => template
      case Some(data) =>
        data.defs.foreach(_.symbol.addAnnotation(Annotation(defn.ScalaStaticAnnot)))
        cpy.Template(template)(body = addInFront(data.defs, template.body))
    }

  }

  private def addInFront(prefix: List[Tree], stats: List[Tree]) = stats match {
    case first :: rest if isSuperConstrCall(first) => first :: prefix ::: rest
    case _ => prefix ::: stats
  }

  /** Make an eager val that would implement synthetic module.
    * Eager val ensures thread safety and has less code generated.
    *
    */
  def transformSyntheticModule(tree: ValOrDefDef)(implicit ctx: Context): Thicket = {
    val sym = tree.symbol
    val holderSymbol = ctx.newSymbol(sym.owner, LazyLocalName.fresh(sym.asTerm.name),
      Flags.Synthetic, sym.info.widen.resultType).enteredAfter(this)
    val field = ValDef(holderSymbol, tree.rhs.changeOwnerAfter(sym, holderSymbol, this))
    val getter = DefDef(sym.asTerm, ref(holderSymbol))
    Thicket(field, getter)
  }

  /** Desugar a local `lazy val x: Int = <RHS>` into:
   *
   *  ```
   *  val x$lzy = new scala.runtime.LazyInt()
   *
   *  def x$lzycompute(): Int = x$lzy.synchronized {
   *    if (x$lzy.initialized()) x$lzy.value()
   *    else x$lzy.initialize(<RHS>)
   *      // TODO: Implement Unit-typed lazy val optimization described below
   *      // for a Unit-typed lazy val, this becomes `{ rhs ; x$lzy.initialize() }`
   *      // to avoid passing around BoxedUnit
   *  }
   *
   *  def x(): Int = if (x$lzy.initialized()) x$lzy.value() else x$lzycompute()
   *  ```
   */
  def transformLocalDef(x: ValOrDefDef)(implicit ctx: Context): Thicket = {
    val xname = x.name.asTermName
    val tpe = x.tpe.widen.resultType.widen

    // val x$lzy = new scala.runtime.LazyInt()
    val holderName = LazyLocalName.fresh(xname)
    val holderImpl = defn.LazyHolder()(ctx)(tpe.typeSymbol)
    val holderSymbol = ctx.newSymbol(x.symbol.owner, holderName, containerFlags, holderImpl.typeRef, coord = x.pos)
    val holderTree = ValDef(holderSymbol, New(holderImpl.typeRef, Nil))

    val holderRef = ref(holderSymbol)
    val getValue = holderRef.select(lazyNme.value).ensureApplied.withPos(x.pos)
    val initialized = holderRef.select(lazyNme.initialized).ensureApplied

    // def x$lzycompute(): Int = x$lzy.synchronized {
    //   if (x$lzy.initialized()) x$lzy.value()
    //   else x$lzy.initialize(<RHS>)
    // }
    val initName = LazyLocalInitName.fresh(xname)
    val initSymbol = ctx.newSymbol(x.symbol.owner, initName, initFlags, MethodType(Nil, tpe), coord = x.pos)
    val rhs = x.rhs.changeOwnerAfter(x.symbol, initSymbol, this)
    val initialize = holderRef.select(lazyNme.initialize).appliedTo(rhs)
    val initBody = holderRef
      .select(defn.Object_synchronized)
      .appliedToType(tpe)
      .appliedTo(If(initialized, getValue, initialize).ensureConforms(tpe))
    val initTree = DefDef(initSymbol, initBody)

    // def x(): Int = if (x$lzy.initialized()) x$lzy.value() else x$lzycompute()
    val accessorBody = If(initialized, getValue, ref(initSymbol).ensureApplied).ensureConforms(tpe)
    val accessor = DefDef(x.symbol.asTerm, accessorBody)

    ctx.debuglog(s"found a lazy val ${x.show},\nrewrote with ${holderTree.show}")
    Thicket(holderTree, initTree, accessor)
  }


  override def transformStats(trees: List[tpd.Tree])(implicit ctx: Context): List[Tree] = {
    // backend requires field usage to be after field definition
    // need to bring containers to start of method
    val (holders, stats) =
      trees.partition {
        _.symbol.flags.&~(Flags.Touched) == containerFlags
        // Filtering out Flags.Touched is not required currently, as there are no LazyTypes involved here
        // but just to be more safe
      }
    holders:::stats
  }

  private def nullOut(nullables: List[Symbol])(implicit ctx: Context): List[Tree] = {
    val nullConst = Literal(Constant(null))
    nullables.map { field =>
      assert(field.isField)
      field.setFlag(Flags.Mutable)
      ref(field).becomes(nullConst)
    }
  }

  /** Create non-threadsafe lazy accessor equivalent to such code
    * ```
    * def methodSymbol() = {
    *   if (!flag) {
    *     target = rhs
    *     flag = true
    *     nullable = null
    *   }
    *   target
    * }
    * ```
    */
  def mkNonThreadSafeDef(sym: Symbol, flag: Symbol, target: Symbol, rhs: Tree)(implicit ctx: Context): DefDef = {
    val targetRef = ref(target)
    val flagRef = ref(flag)
    val stats = targetRef.becomes(rhs) :: flagRef.becomes(Literal(Constant(true))) :: nullOut(nullableFor(sym))
    val init = If(
      flagRef.ensureApplied.select(nme.UNARY_!).ensureApplied,
      Block(stats.init, stats.last),
      unitLiteral
    )
    DefDef(sym.asTerm, Block(List(init), targetRef.ensureApplied))
  }

  /** Create non-threadsafe lazy accessor for not-nullable types  equivalent to such code
    * ```
    * def methodSymbol() = {
    *   if (target eq null) {
    *     target = rhs
    *     nullable = null
    *   }
    *   target
    * }
    * ```
    */
  def mkDefNonThreadSafeNonNullable(sym: Symbol, target: Symbol, rhs: Tree)(implicit ctx: Context): DefDef = {
    val targetRef = ref(target)
    val stats = targetRef.becomes(rhs) :: nullOut(nullableFor(sym))
    val init = If(
      targetRef.select(nme.eq).appliedTo(Literal(Constant(null))),
      Block(stats.init, stats.last),
      unitLiteral
    )
    DefDef(sym.asTerm, Block(List(init), targetRef.ensureApplied))
  }

  def transformMemberDefNonVolatile(x: ValOrDefDef)(implicit ctx: Context): Thicket = {
    val claz = x.symbol.owner.asClass
    val tpe = x.tpe.widen.resultType.widen
    assert(!(x.symbol is Flags.Mutable))
    val containerName = LazyLocalName.fresh(x.name.asTermName)
    val containerSymbol = ctx.newSymbol(claz, containerName,
      x.symbol.flags &~ containerFlagsMask | containerFlags | Flags.Private,
      tpe, coord = x.symbol.coord
    ).enteredAfter(this)

    val containerTree = ValDef(containerSymbol, defaultValue(tpe))
    if (x.tpe.isNotNull && tpe <:< defn.ObjectType) {
      // can use 'null' value instead of flag
      Thicket(containerTree, mkDefNonThreadSafeNonNullable(x.symbol, containerSymbol, x.rhs))
    }
    else {
      val flagName = LazyBitMapName.fresh(x.name.asTermName)
      val flagSymbol = ctx.newSymbol(x.symbol.owner, flagName,  containerFlags | Flags.Private, defn.BooleanType).enteredAfter(this)
      val flag = ValDef(flagSymbol, Literal(Constant(false)))
      Thicket(containerTree, flag, mkNonThreadSafeDef(x.symbol, flagSymbol, containerSymbol, x.rhs))
    }
  }

  /** Create a threadsafe lazy accessor equivalent to such code
    * ```
    * def methodSymbol(): Int = {
    *   val result: Int = 0
    *   val retry: Boolean = true
    *   var flag: Long = 0L
    *   while retry do {
    *     flag = dotty.runtime.LazyVals.get(this, $claz.$OFFSET)
    *     dotty.runtime.LazyVals.STATE(flag, 0) match {
    *       case 0 =>
    *         if dotty.runtime.LazyVals.CAS(this, $claz.$OFFSET, flag, 1, $ord) {
    *           try {result = rhs} catch {
    *             case x: Throwable =>
    *               dotty.runtime.LazyVals.setFlag(this, $claz.$OFFSET, 0, $ord)
    *               throw x
    *           }
    *           $target = result
    *           dotty.runtime.LazyVals.setFlag(this, $claz.$OFFSET, 3, $ord)
    *           retry = false
    *           }
    *       case 1 =>
    *         dotty.runtime.LazyVals.wait4Notification(this, $claz.$OFFSET, flag, $ord)
    *       case 2 =>
    *         dotty.runtime.LazyVals.wait4Notification(this, $claz.$OFFSET, flag, $ord)
    *       case 3 =>
    *         retry = false
    *         result = $target
    *       }
    *     }
    *   nullable = null
    *   result
    * }
    * ```
    */
  def mkThreadSafeDef(methodSymbol: TermSymbol,
                      claz: ClassSymbol,
                      ord: Int,
                      target: Symbol,
                      rhs: Tree,
                      tp: Type,
                      offset: Tree,
                      getFlag: Tree,
                      stateMask: Tree,
                      casFlag: Tree,
                      setFlagState: Tree,
                      waitOnLock: Tree,
                      nullables: List[Symbol])(implicit ctx: Context): DefDef = {
    val initState = Literal(Constant(0))
    val computeState = Literal(Constant(1))
    val notifyState = Literal(Constant(2))
    val computedState = Literal(Constant(3))
    val flagSymbol = ctx.newSymbol(methodSymbol, lazyNme.flag, containerFlags, defn.LongType)
    val flagDef = ValDef(flagSymbol, Literal(Constant(0L)))

    val thiz = This(claz)(ctx.fresh.setOwner(claz))

    val resultSymbol = ctx.newSymbol(methodSymbol, lazyNme.result, containerFlags, tp)
    val resultDef = ValDef(resultSymbol, defaultValue(tp))

    val retrySymbol = ctx.newSymbol(methodSymbol, lazyNme.retry, containerFlags, defn.BooleanType)
    val retryDef = ValDef(retrySymbol, Literal(Constant(true)))

    val whileCond = ref(retrySymbol)

    val compute = {
      val handlerSymbol = ctx.newSymbol(methodSymbol, nme.ANON_FUN, Flags.Synthetic,
        MethodType(List(nme.x_1), List(defn.ThrowableType), defn.IntType))
      val caseSymbol = ctx.newSymbol(methodSymbol, nme.DEFAULT_EXCEPTION_NAME, Flags.Synthetic, defn.ThrowableType)
      val triggerRetry = setFlagState.appliedTo(thiz, offset, initState, Literal(Constant(ord)))
      val complete = setFlagState.appliedTo(thiz, offset, computedState, Literal(Constant(ord)))

      val handler = CaseDef(Bind(caseSymbol, ref(caseSymbol)), EmptyTree,
        Block(List(triggerRetry), Throw(ref(caseSymbol))
      ))

      val compute = ref(resultSymbol).becomes(rhs)
      val tr = Try(compute, List(handler), EmptyTree)
      val assign = ref(target).becomes(ref(resultSymbol))
      val noRetry = ref(retrySymbol).becomes(Literal(Constant(false)))
      val body = If(casFlag.appliedTo(thiz, offset, ref(flagSymbol), computeState, Literal(Constant(ord))),
        Block(tr :: assign :: complete :: noRetry :: Nil, Literal(Constant(()))),
        Literal(Constant(())))

      CaseDef(initState, EmptyTree, body)
    }

    val waitFirst = {
      val wait = waitOnLock.appliedTo(thiz, offset, ref(flagSymbol), Literal(Constant(ord)))
      CaseDef(computeState, EmptyTree, wait)
    }

    val waitSecond = {
      val wait = waitOnLock.appliedTo(thiz, offset, ref(flagSymbol), Literal(Constant(ord)))
      CaseDef(notifyState, EmptyTree, wait)
    }

    val computed = {
      val noRetry = ref(retrySymbol).becomes(Literal(Constant(false)))
      val result = ref(resultSymbol).becomes(ref(target))
      val body = Block(noRetry :: result :: Nil, Literal(Constant(())))
      CaseDef(computedState, EmptyTree, body)
    }

    val default = CaseDef(Underscore(defn.LongType), EmptyTree, Literal(Constant(())))

    val cases = Match(stateMask.appliedTo(ref(flagSymbol), Literal(Constant(ord))),
      List(compute, waitFirst, waitSecond, computed, default)) //todo: annotate with @switch

    val whileBody = Block(ref(flagSymbol).becomes(getFlag.appliedTo(thiz, offset)) :: Nil, cases)
    val cycle = WhileDo(whileCond, whileBody)
    val setNullables = nullOut(nullables)
    DefDef(methodSymbol, Block(resultDef :: retryDef :: flagDef :: cycle :: setNullables, ref(resultSymbol)))
  }

  def transformMemberDefVolatile(x: ValOrDefDef)(implicit ctx: Context): Thicket = {
    assert(!(x.symbol is Flags.Mutable))

    val tpe = x.tpe.widen.resultType.widen
    val claz = x.symbol.owner.asClass
    val thizClass = Literal(Constant(claz.info))
    val helperModule = ctx.requiredModule("dotty.runtime.LazyVals")
    val getOffset = Select(ref(helperModule), lazyNme.RLazyVals.getOffset)
    var offsetSymbol: TermSymbol = null
    var flag: Tree = EmptyTree
    var ord = 0

    def offsetName(id: Int) = (StdNames.nme.LAZY_FIELD_OFFSET + (if(x.symbol.owner.is(Flags.Module)) "_m_" else "") + id.toString).toTermName

    // compute or create appropriate offsetSymol, bitmap and bits used by current ValDef
    appendOffsetDefs.get(claz) match {
      case Some(info) =>
        val flagsPerLong = (64 / dotty.runtime.LazyVals.BITS_PER_LAZY_VAL).toInt
        info.ord += 1
        ord = info.ord % flagsPerLong
        val id = info.ord / flagsPerLong
        val offsetById = offsetName(id)
        if (ord != 0) { // there are unused bits in already existing flag
          offsetSymbol = claz.info.decl(offsetById)
            .suchThat(sym => (sym is Flags.Synthetic) && sym.isTerm)
             .symbol.asTerm
        } else { // need to create a new flag
          offsetSymbol = ctx.newSymbol(claz, offsetById, Flags.Synthetic, defn.LongType).enteredAfter(this)
          offsetSymbol.addAnnotation(Annotation(defn.ScalaStaticAnnot))
          val flagName = (StdNames.nme.BITMAP_PREFIX + id.toString).toTermName
          val flagSymbol = ctx.newSymbol(claz, flagName, containerFlags, defn.LongType).enteredAfter(this)
          flag = ValDef(flagSymbol, Literal(Constant(0L)))
          val offsetTree = ValDef(offsetSymbol, getOffset.appliedTo(thizClass, Literal(Constant(flagName.toString))))
          info.defs = offsetTree :: info.defs
        }

      case None =>
        offsetSymbol = ctx.newSymbol(claz, offsetName(0), Flags.Synthetic, defn.LongType).enteredAfter(this)
        offsetSymbol.addAnnotation(Annotation(defn.ScalaStaticAnnot))
        val flagName = (StdNames.nme.BITMAP_PREFIX + "0").toTermName
        val flagSymbol = ctx.newSymbol(claz, flagName, containerFlags, defn.LongType).enteredAfter(this)
        flag = ValDef(flagSymbol, Literal(Constant(0L)))
        val offsetTree = ValDef(offsetSymbol, getOffset.appliedTo(thizClass, Literal(Constant(flagName.toString))))
        appendOffsetDefs += (claz -> new OffsetInfo(List(offsetTree), ord))
    }

    val containerName = LazyLocalName.fresh(x.name.asTermName)
    val containerSymbol = ctx.newSymbol(claz, containerName, x.symbol.flags &~ containerFlagsMask | containerFlags, tpe, coord = x.symbol.coord).enteredAfter(this)

    val containerTree = ValDef(containerSymbol, defaultValue(tpe))

    val offset =  ref(offsetSymbol)
    val getFlag = Select(ref(helperModule), lazyNme.RLazyVals.get)
    val setFlag = Select(ref(helperModule), lazyNme.RLazyVals.setFlag)
    val wait =    Select(ref(helperModule), lazyNme.RLazyVals.wait4Notification)
    val state =   Select(ref(helperModule), lazyNme.RLazyVals.state)
    val cas =     Select(ref(helperModule), lazyNme.RLazyVals.cas)
    val nullables = nullableFor(x.symbol)

    val accessor = mkThreadSafeDef(x.symbol.asTerm, claz, ord, containerSymbol, x.rhs, tpe, offset, getFlag, state, cas, setFlag, wait, nullables)
    if (flag eq EmptyTree)
      Thicket(containerTree, accessor)
    else Thicket(containerTree, flag, accessor)
  }
}

object LazyVals {
  object lazyNme {
    import Names.TermName
    object RLazyVals {
      import dotty.runtime.LazyVals.{Names => N}
      val get: TermName               = N.get.toTermName
      val setFlag: TermName           = N.setFlag.toTermName
      val wait4Notification: TermName = N.wait4Notification.toTermName
      val state: TermName             = N.state.toTermName
      val cas: TermName               = N.cas.toTermName
      val getOffset: TermName         = N.getOffset.toTermName
    }
    val flag: TermName        = "flag".toTermName
    val result: TermName      = "result".toTermName
    val value: TermName       = "value".toTermName
    val initialized: TermName = "initialized".toTermName
    val initialize: TermName  = "initialize".toTermName
    val retry: TermName       = "retry".toTermName
  }
}



