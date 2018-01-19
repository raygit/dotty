package dotty.tools.dotc.core.quoted

import dotty.tools.dotc.ast.Trees._
import dotty.tools.dotc.ast.{tpd, untpd}
import dotty.tools.dotc.config.Printers._
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts._
import dotty.tools.dotc.core.Decorators._
import dotty.tools.dotc.core.Flags._
import dotty.tools.dotc.core.StdNames._
import dotty.tools.dotc.core.Symbols._
import dotty.tools.dotc.core.tasty.{TastyPickler, TastyPrinter, TastyString}
import dotty.tools.dotc.interpreter.RawQuoted

import scala.reflect.ClassTag

object PickledQuotes {
  import tpd._

  /** Pickle the quote into strings */
  def pickleQuote(tree: Tree)(implicit ctx: Context): Tree = {
    if (ctx.reporter.hasErrors) Literal(Constant("<error>"))
    else {
      val encapsulated = encapsulateQuote(tree)
      val pickled = pickle(encapsulated)
      TastyString.pickle(pickled).foldRight[Tree](ref(defn.NilModule)) { (x, acc) =>
        acc.select("::".toTermName)
          .appliedToType(defn.StringType)
          .appliedTo(Literal(Constant(x)))
      }
    }
  }

  /** Transform the expression into its fully spliced Tree */
  def quotedToTree(expr: quoted.Quoted)(implicit ctx: Context): Tree = expr match {
    case expr: quoted.TastyQuoted => unpickleQuote(expr)
    case expr: quoted.Liftable.ConstantExpr[_] => Literal(Constant(expr.value))
    case expr: quoted.Type.TaggedPrimitive[_] =>
      val tpe = expr.ct match {
        case ClassTag.Unit => defn.UnitType
        case ClassTag.Byte => defn.ByteType
        case ClassTag.Char => defn.CharType
        case ClassTag.Short => defn.ShortType
        case ClassTag.Int => defn.IntType
        case ClassTag.Long => defn.LongType
        case ClassTag.Float => defn.FloatType
        case ClassTag.Double => defn.FloatType
      }
      TypeTree(tpe)
    case expr: RawQuoted => expr.tree
  }

  /** Unpickle the tree contained in the TastyQuoted */
  private def unpickleQuote(expr: quoted.TastyQuoted)(implicit ctx: Context): Tree = {
    val tastyBytes = TastyString.unpickle(expr.tasty)
    val unpickled = unpickle(tastyBytes, expr.args)
    unpickled match {
      case PackageDef(_, (vdef: ValDef) :: Nil) =>
        if (vdef.name == "$quote".toTermName) vdef.rhs
        else vdef.rhs.asInstanceOf[TypeApply].args.head
    }
  }

  /** Encapsulate the tree in a top level `val` or `type`
   *    `<tree>` ==> `package _root_ { val $quote: Any = <tree> }`
   *    or
   *    `<type tree>` ==> `package _root_ { val $typeQuote: Any = null.asInstanceOf[<tree>] }`
   */
  private def encapsulateQuote(tree: Tree)(implicit ctx: Context): Tree = {
    val name = (if (tree.isTerm) "$quote" else "$typeQuote").toTermName
    val sym = ctx.newSymbol(ctx.owner, name, Synthetic, defn.AnyType, coord = tree.pos)
    val encoded =
      if (tree.isTerm) tree
      else Literal(Constant(null)).select(nme.asInstanceOf_).appliedToTypeTrees(tree :: Nil)
    val quoted = ValDef(sym, encoded).withPos(tree.pos)
    PackageDef(ref(defn.RootPackage).asInstanceOf[Ident], quoted :: Nil).withPos(tree.pos)
  }

  // TASTY picklingtests/pos/quoteTest.scala

  /** Pickle tree into it's TASTY bytes s*/
  private def pickle(tree: Tree)(implicit ctx: Context): Array[Byte] = {
    val pickler = new TastyPickler(defn.RootClass)
    val treePkl = pickler.treePkl
    treePkl.pickle(tree :: Nil)
    treePkl.compactify()
    pickler.addrOfTree = treePkl.buf.addrOfTree
    pickler.addrOfSym = treePkl.addrOfSym
    // if (tree.pos.exists)
    //   new PositionPickler(pickler, treePkl.buf.addrOfTree).picklePositions(tree :: Nil)

    // other pickle sections go here.
    val pickled = pickler.assembleParts()

    if (pickling ne noPrinter) {
      println(i"**** pickled quote of \n${tree.show}")
      new TastyPrinter(pickled).printContents()
    }

    pickled
  }

  /** Unpickle TASTY bytes into it's tree */
  private def unpickle(bytes: Array[Byte], splices: Seq[Any])(implicit ctx: Context): Tree = {
    val unpickler = new TastyUnpickler(bytes, splices)
    unpickler.enter(roots = Set(defn.RootPackage))
    val tree = unpickler.body.head
    if (pickling ne noPrinter) {
      println(i"**** unpickled quote for \n${tree.show}")
      new TastyPrinter(bytes).printContents()
    }
    tree
  }
}
