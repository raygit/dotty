package dotty.tools.dotc.tastyreflect

import dotty.tools.dotc.ast.{Trees, tpd, untpd}
import dotty.tools.dotc.core
import dotty.tools.dotc.core.Decorators._
import dotty.tools.dotc.core._
import dotty.tools.dotc.tastyreflect.FromSymbol.{definitionFromSym, packageDefFromSym}

trait TreeOpsImpl extends scala.tasty.reflect.TreeOps with CoreImpl with Helpers {

  def TreeDeco(tree: Tree): TreeAPI = new TreeAPI {
    def pos(implicit ctx: Context): Position = tree.pos
    def symbol(implicit ctx: Context): Symbol = tree.symbol
  }

  def PackageClauseDeco(pack: PackageClause): PackageClauseAPI = new PackageClauseAPI {
  }

  def ImportDeco(imp: Import): ImportAPI = new ImportAPI {
    def expr(implicit ctx: Context): Tree = imp.expr
    def selector(implicit ctx: Context): List[ImportSelector] = imp.selectors
  }

  def DefinitionDeco(definition: Definition): DefinitionAPI = new DefinitionAPI {
    def name(implicit ctx: Context): String = definition.symbol.name.toString
  }

  def ClassDefDeco(cdef: ClassDef): ClassDefAPI = new ClassDefAPI {
    private def rhs = cdef.rhs.asInstanceOf[tpd.Template]
    def constructor(implicit ctx: Context): DefDef = rhs.constr
    def parents(implicit ctx: Context): List[TermOrTypeTree] = rhs.parents
    def self(implicit ctx: Context): Option[tpd.ValDef] = optional(rhs.self)
    def body(implicit ctx: Context): List[Statement] = rhs.body
    def symbol(implicit ctx: Context): ClassSymbol = cdef.symbol.asClass
  }

  def DefDefDeco(ddef: DefDef): DefDefAPI = new DefDefAPI {
    def typeParams(implicit ctx: Context): List[TypeDef] = ddef.tparams
    def paramss(implicit ctx: Context): List[List[ValDef]] = ddef.vparamss
    def returnTpt(implicit ctx: Context): TypeTree = ddef.tpt
    def rhs(implicit ctx: Context): Option[Tree] = optional(ddef.rhs)
    def symbol(implicit ctx: Context): DefSymbol = ddef.symbol.asTerm
  }

  def ValDefDeco(vdef: ValDef): ValDefAPI = new ValDefAPI {
    def tpt(implicit ctx: Context): TypeTree = vdef.tpt
    def rhs(implicit ctx: Context): Option[Tree] = optional(vdef.rhs)
    def symbol(implicit ctx: Context): ValSymbol = vdef.symbol.asTerm
  }
  def TypeDefDeco(tdef: TypeDef): TypeDefAPI = new TypeDefAPI {
    def rhs(implicit ctx: Context): TypeOrBoundsTree = tdef.rhs
    def symbol(implicit ctx: Context): TypeSymbol = tdef.symbol.asType
  }

  def PackageDefDeco(pdef: PackageDef): PackageDefAPI = new PackageDefAPI {

    def owner(implicit ctx: Context): PackageDefinition = packageDefFromSym(pdef.symbol.owner)

    def members(implicit ctx: Context): List[Statement] = {
      if (pdef.symbol.is(core.Flags.JavaDefined)) Nil // FIXME should also support java packages
      else pdef.symbol.info.decls.iterator.map(definitionFromSym).toList
    }

    def symbol(implicit ctx: Context): PackageSymbol = pdef.symbol
  }

  def IdentDeco(x: Term.Ident): Term.IdentAPI = new Term.IdentAPI {
    def name(implicit ctx: Context): String = x.name.show
  }

  def SelectDeco(x: Term.Select): Term.SelectAPI = new Term.SelectAPI {
    def qualifier(implicit ctx: Context): Term = x.qualifier
    def name(implicit ctx: Context): String = x.name.toString
    def signature(implicit ctx: Context): Option[Signature] =
      if (x.symbol.signature == core.Signature.NotAMethod) None
      else Some(x.symbol.signature)
  }

  def LiteralDeco(x: Term.Literal): Term.LiteralAPI = new Term.LiteralAPI {
    def constant(implicit ctx: Context): Constant = x.const
  }

  def ThisDeco(x: Term.This): Term.ThisAPI = new Term.ThisAPI {
    def id(implicit ctx: Context): Option[Id] = optional(x.qual)
  }

  def NewDeco(x: Term.New): Term.NewAPI = new Term.NewAPI {
    def tpt(implicit ctx: Context): TypeTree = x.tpt
  }

  def NamedArgDeco(x: Term.NamedArg): Term.NamedArgAPI = new Term.NamedArgAPI {
    def name(implicit ctx: Context): String = x.name.toString
    def value(implicit ctx: Context): Term = x.arg
  }

  def ApplyDeco(x: Term.Apply): Term.ApplyAPI = new Term.ApplyAPI {
    def fun(implicit ctx: Context): Term = x.fun
    def args(implicit ctx: Context): List[Term] = x.args
  }

  def TypeApplyDeco(x: Term.TypeApply): Term.TypeApplyAPI = new Term.TypeApplyAPI {
    def fun(implicit ctx: Context): Term = x.fun
    def args(implicit ctx: Context): List[TypeTree] = x.args
  }

  def SuperDeco(x: Term.Super): Term.SuperAPI = new Term.SuperAPI {
    def qualifier(implicit ctx: Context): Term = x.qual
    def id(implicit ctx: Context): Option[untpd.Ident] = optional(x.mix)
  }

  def TypedDeco(x: Term.Typed): Term.TypedAPI = new Term.TypedAPI {
    def expr(implicit ctx: Context): Term = x.expr
    def tpt(implicit ctx: Context): TypeTree = x.tpt
  }

  def AssignDeco(x: Term.Assign): Term.AssignAPI = new Term.AssignAPI {
    def lhs(implicit ctx: Context): Term = x.lhs
    def rhs(implicit ctx: Context): Term = x.rhs
  }

  def BlockDeco(x: Term.Block): Term.BlockAPI = new Term.BlockAPI {
    def statements(implicit ctx: Context): List[Statement] = x.stats
    def expr(implicit ctx: Context): Term = x.expr
  }

  def InlinedDeco(x: Term.Inlined): Term.InlinedAPI = new Term.InlinedAPI {
    def call(implicit ctx: Context): Option[Term] = optional(x.call)
    def bindings(implicit ctx: Context): List[Definition] = x.bindings
    def body(implicit ctx: Context): Term = x.expansion
  }

  def LambdaDeco(x: Term.Lambda): Term.LambdaAPI = new Term.LambdaAPI {
    def meth(implicit ctx: Context): Term = x.meth
    def tptOpt(implicit ctx: Context): Option[TypeTree] = optional(x.tpt)
  }

  def IfDeco(x: Term.If): Term.IfAPI = new Term.IfAPI {
    def cond(implicit ctx: Context): Term = x.cond
    def thenp(implicit ctx: Context): Term = x.thenp
    def elsep(implicit ctx: Context): Term = x.elsep
  }

  def MatchDeco(x: Term.Match): Term.MatchAPI = new Term.MatchAPI {
    def scrutinee(implicit ctx: Context): Term = x.selector
    def cases(implicit ctx: Context): List[tpd.CaseDef] = x.cases
  }

  def TryDeco(x: Term.Try): Term.TryAPI = new Term.TryAPI {
    def body(implicit ctx: Context): Term = x.expr
    def cases(implicit ctx: Context): List[CaseDef] = x.cases
    def finalizer(implicit ctx: Context): Option[Term] = optional(x.finalizer)
  }

  def ReturnDeco(x: Term.Return): Term.ReturnAPI = new Term.ReturnAPI {
    def expr(implicit ctx: Context): Term = x.expr
  }

  def RepeatedDeco(x: Term.Repeated): Term.RepeatedAPI = new Term.RepeatedAPI {
    def elems(implicit ctx: Context): List[Term] = x.elems
  }

  def SelectOuterDeco(x: Term.SelectOuter): Term.SelectOuterAPI = new Term.SelectOuterAPI {
    def qualifier(implicit ctx: Context): Term = x.qualifier
    def level(implicit ctx: Context): Int = {
      val NameKinds.OuterSelectName(_, levels) = x.name
      levels
    }
    def tpe(implicit ctx: Context): Type = x.tpe.stripTypeVar
  }

  def WhileDeco(x: Term.While): Term.WhileAPI = new Term.WhileAPI {
    def cond(implicit ctx: Context): Term = x.cond
    def body(implicit ctx: Context): Term = x.body
  }

  // ----- Tree ----------------------------------------------------

  object IsPackageClause extends IsPackageClauseModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[PackageClause] = tree match {
      case x: tpd.PackageDef => Some(x)
      case _ => None
    }
  }

  object PackageClause extends PackageClauseModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[Tree])] = tree match {
      case x: tpd.PackageDef => Some((x.pid, x.stats))
      case _ => None
    }
  }

  // ----- Statements -----------------------------------------------

  object Import extends ImportModule {
    def unapply(x: Tree)(implicit ctx: Context): Option[(Term, List[ImportSelector])] = x match {
      case x: tpd.Import => Some((x.expr, x.selectors))
      case _ => None
    }
  }

  // ----- Definitions ----------------------------------------------

  object IsDefinition extends IsDefinitionModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[Definition] = tree match {
      case tree: tpd.MemberDef => Some(tree)
      case tree: PackageDefinition => Some(tree)
      case _ => None
    }
  }


  // ClassDef

  object IsClassDef extends IsClassDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[ClassDef] = tree match {
      case x: tpd.TypeDef if x.isClassDef => Some(x)
      case _ => None
    }
  }

  object ClassDef extends ClassDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, DefDef, List[TermOrTypeTree],  Option[ValDef], List[Statement])] = tree match {
      case Trees.TypeDef(name, impl: tpd.Template) =>
        Some((name.toString, impl.constr, impl.parents, optional(impl.self), impl.body))
      case _ => None
    }
  }


  // DefDef

  object IsDefDef extends IsDefDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[DefDef] = tree match {
      case x: tpd.DefDef => Some(x)
      case _ => None
    }
  }

  object DefDef extends DefDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, List[TypeDef],  List[List[ValDef]], TypeTree, Option[Term])] = tree match {
      case x: tpd.DefDef =>
        Some((x.name.toString, x.tparams, x.vparamss, x.tpt, optional(x.rhs)))
      case _ => None
    }
  }


  // ValDef

  object IsValDef extends IsValDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[ValDef] = tree match {
      case x: tpd.ValDef => Some(x)
      case _ => None
    }
  }

  object ValDef extends ValDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, TypeTree, Option[Term])] = tree match {
      case x: tpd.ValDef =>
        Some((x.name.toString, x.tpt, optional(x.rhs)))
      case _ => None
    }
  }


  // TypeDef

  object IsTypeDef extends IsTypeDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[TypeDef] = tree match {
      case x: tpd.TypeDef if !x.symbol.isClass => Some(x)
      case _ => None
    }
  }

  object TypeDef extends TypeDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, TypeOrBoundsTree /* TypeTree | TypeBoundsTree */)] = tree match {
      case x: tpd.TypeDef if !x.symbol.isClass => Some((x.name.toString, x.rhs))
      case _ => None
    }
  }


  // PackageDef


  object IsPackageDef extends IsPackageDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[PackageDef] = tree match {
      case x: PackageDefinition => Some(x)
      case _ => None
    }
  }

  object PackageDef extends PackageDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, PackageDef)] = tree match {
      case x: PackageDefinition =>
        Some((x.symbol.name.toString, packageDefFromSym(x.symbol.owner)))
      case _ => None
    }
  }

  // ----- Terms ----------------------------------------------------

  def TermDeco(term: Term): TermAPI = new TermAPI {
    import tpd._
    def pos(implicit ctx: Context): Position = term.pos
    def tpe(implicit ctx: Context): Type = term.tpe
    def underlyingArgument(implicit ctx: Context): Term = term.underlyingArgument
    def underlying(implicit ctx: Context): Term = term.underlying
  }

  object IsTerm extends IsTermModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[Term] =
      if (tree.isTerm) Some(tree) else None
    def unapply(termOrTypeTree: TermOrTypeTree)(implicit ctx: Context, dummy: DummyImplicit): Option[Term] =
      if (termOrTypeTree.isTerm) Some(termOrTypeTree) else None
  }

  object Term extends TermModule with TermCoreModuleImpl {

    object IsIdent extends IsIdentModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Ident] = x match {
        case x: tpd.Ident if x.isTerm => Some(x)
        case _ => None
      }
    }


    object Ident extends IdentModule {
      def unapply(x: Term)(implicit ctx: Context): Option[String] = x match {
        case x: tpd.Ident if x.isTerm => Some(x.name.show)
        case _ => None
      }
    }

    object IsSelect extends IsSelectModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Select] = x match {
        case x: tpd.Select if x.isTerm => Some(x)
        case _ => None
      }
    }


    object Select extends SelectModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, String)] = x match {
        case x: tpd.Select if x.isTerm => Some((x.qualifier, x.name.toString))
        case _ => None
      }
    }

    object IsLiteral extends IsLiteralModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Literal] = x match {
        case x: tpd.Literal => Some(x)
        case _ => None
      }
    }



    object Literal extends LiteralModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Constant] = x match {
        case Trees.Literal(const) => Some(const)
        case _ => None
      }
    }

    object IsThis extends IsThisModule {
      def unapply(x: Term)(implicit ctx: Context): Option[This] = x match {
        case x: tpd.This => Some(x)
        case _ => None
      }
    }


    object This extends ThisModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Option[Id]] = x match {
        case Trees.This(qual) => Some(optional(qual))
        case _ => None
      }
    }

    object IsNew extends IsNewModule {
      def unapply(x: Term)(implicit ctx: Context): Option[New] = x match {
        case x: tpd.New => Some(x)
        case _ => None
      }
    }



    object New extends NewModule {
      def unapply(x: Term)(implicit ctx: Context): Option[TypeTree] = x match {
        case x: tpd.New => Some(x.tpt)
        case _ => None
      }
    }

    object IsNamedArg extends IsNamedArgModule {
      def unapply(x: Term)(implicit ctx: Context): Option[NamedArg] = x match {
        case x: tpd.NamedArg if x.name.isInstanceOf[Names.TermName] => Some(x) // TODO: Now, the name should alwas be a term name
        case _ => None
      }
    }


    object NamedArg extends NamedArgModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(String, Term)] = x match {
        case x: tpd.NamedArg if x.name.isInstanceOf[Names.TermName] => Some((x.name.toString, x.arg))
        case _ => None
      }
    }

    object IsApply extends IsApplyModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Apply] = x match {
        case x: tpd.Apply => Some(x)
        case _ => None
      }
    }


    object Apply extends ApplyModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, List[Term])] = x match {
        case x: tpd.Apply => Some((x.fun, x.args))
        case _ => None
      }
    }

    object IsTypeApply extends IsTypeApplyModule {
      def unapply(x: Term)(implicit ctx: Context): Option[TypeApply] = x match {
        case x: tpd.TypeApply => Some(x)
        case _ => None
      }
    }


    object TypeApply extends TypeApplyModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, List[TypeTree])] = x match {
        case x: tpd.TypeApply => Some((x.fun, x.args))
        case _ => None
      }
    }

    object IsSuper extends IsSuperModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Super] = x match {
        case x: tpd.Super => Some(x)
        case _ => None
      }
    }


    object Super extends SuperModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, Option[Id])] = x match {
        case x: tpd.Super => Some((x.qual, if (x.mix.isEmpty) None else Some(x.mix)))
        case _ => None
      }
    }

    object IsTyped extends IsTypedModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Typed] = x match {
        case x: tpd.Typed => Some(x)
        case _ => None
      }
    }


    object Typed extends TypedModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, TypeTree)] = x match {
        case x: tpd.Typed => Some((x.expr, x.tpt))
        case _ => None
      }
    }

    object IsAssign extends IsAssignModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Assign] = x match {
        case x: tpd.Assign => Some(x)
        case _ => None
      }
    }


    object Assign extends AssignModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, Term)] = x match {
        case x: tpd.Assign => Some((x.lhs, x.rhs))
        case _ => None
      }
    }

    object IsBlock extends IsBlockModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Block] = normalizedLoops(x) match {
        case x: tpd.Block => Some(x)
        case _ => None
      }

      /** Normalizes non Blocks.
       *  i) Put `while` and `doWhile` loops in their own blocks: `{ def while$() = ...; while$() }`
       *  ii) Put closures in their own blocks: `{ def anon$() = ...; closure(anon$, ...) }`
       */
      private def normalizedLoops(tree: tpd.Tree)(implicit ctx: Context): tpd.Tree = tree match {
        case block: tpd.Block if block.stats.size > 1 =>
          def normalizeInnerLoops(stats: List[tpd.Tree]): List[tpd.Tree] = stats match {
            case (x: tpd.DefDef) :: y :: xs if needsNormalization(y) =>
              tpd.Block(x :: Nil, y) :: normalizeInnerLoops(xs)
            case x :: xs => x :: normalizeInnerLoops(xs)
            case Nil => Nil
          }
          if (needsNormalization(block.expr)) {
            val stats1 = normalizeInnerLoops(block.stats.init)
            val normalLoop = tpd.Block(block.stats.last :: Nil, block.expr)
            tpd.Block(stats1, normalLoop)
          } else {
            val stats1 = normalizeInnerLoops(block.stats)
            tpd.cpy.Block(block)(stats1, block.expr)
          }
        case _ => tree
      }

      /** If it is the second statement of a closure. See: `normalizedLoops` */
      private def needsNormalization(tree: tpd.Tree)(implicit ctx: Context): Boolean = tree match {
        case _: tpd.Closure => true
        case _ => false
      }
    }


    object Block extends BlockModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(List[Statement], Term)] = x match {
        case IsBlock(x) => Some((x.stats, x.expr))
        case _ => None
      }
    }

    object IsInlined extends IsInlinedModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Inlined] = x match {
        case x: tpd.Inlined => Some(x)
        case _ => None
      }
    }


    object Inlined extends InlinedModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Option[TermOrTypeTree], List[Statement], Term)] = x match {
        case x: tpd.Inlined =>
          Some((optional(x.call), x.bindings, x.expansion))
        case _ => None
      }
    }

    object IsLambda extends IsLambdaModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Lambda] = x match {
        case x: tpd.Closure => Some(x)
        case _ => None
      }
    }


    object Lambda extends LambdaModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, Option[TypeTree])] = x match {
        case x: tpd.Closure => Some((x.meth, optional(x.tpt)))
        case _ => None
      }
    }

    object IsIf extends IsIfModule {
      def unapply(x: Term)(implicit ctx: Context): Option[If] = x match {
        case x: tpd.If => Some(x)
        case _ => None
      }
    }


    object If extends IfModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, Term, Term)] = x match {
        case x: tpd.If => Some((x.cond, x.thenp, x.elsep))
        case _ => None
      }
    }

    object IsMatch extends IsMatchModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Match] = x match {
        case x: tpd.Match => Some(x)
        case _ => None
      }
    }


    object Match extends MatchModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, List[CaseDef])] = x match {
        case x: tpd.Match => Some((x.selector, x.cases))
        case _ => None
      }
    }

    object IsTry extends IsTryModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Try] = x match {
        case x: tpd.Try => Some(x)
        case _ => None
      }
    }


    object Try extends TryModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, List[CaseDef], Option[Term])] = x match {
        case x: tpd.Try => Some((x.expr, x.cases, optional(x.finalizer)))
        case _ => None
      }
    }

    object IsReturn extends IsReturnModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Return] = x match {
        case x: tpd.Return => Some(x)
        case _ => None
      }
    }


    object Return extends ReturnModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Term] = x match {
        case x: tpd.Return => Some(x.expr)
        case _ => None
      }
    }

    object IsRepeated extends IsRepeatedModule {
      def unapply(x: Term)(implicit ctx: Context): Option[Repeated] = x match {
        case x: tpd.SeqLiteral => Some(x)
        case _ => None
      }
    }


    object Repeated extends RepeatedModule {
      def unapply(x: Term)(implicit ctx: Context): Option[List[Term]] = x match {
        case x: tpd.SeqLiteral => Some(x.elems)
        case _ => None
      }
    }

    object IsSelectOuter extends IsSelectOuterModule {
      def unapply(x: Term)(implicit ctx: Context): Option[SelectOuter] = x match {
        case x: tpd.Select =>
          x.name match {
            case NameKinds.OuterSelectName(_, _) => Some(x)
            case _ => None
          }
        case _ => None
      }
    }


    object SelectOuter extends SelectOuterModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, Int, Type)] = x match {
        case x: tpd.Select =>
          x.name match {
            case NameKinds.OuterSelectName(_, levels) => Some((x.qualifier, levels, x.tpe.stripTypeVar))
            case _ => None
          }
        case _ => None
      }
    }

    object IsWhile extends IsWhileModule {
      def unapply(x: Term)(implicit ctx: Context): Option[While] = x match {
        case x: tpd.WhileDo => Some(x)
        case _ => None
      }
    }


    object While extends WhileModule {
      def unapply(x: Term)(implicit ctx: Context): Option[(Term, Term)] = x match {
        case x: tpd.WhileDo => Some((x.cond, x.body))
        case _ => None
      }
    }
  }

  def termAsTermOrTypeTree(term: Term): TermOrTypeTree = term
}
