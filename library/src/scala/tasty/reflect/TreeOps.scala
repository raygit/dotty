package scala.tasty
package reflect

trait TreeOps extends Core {

  // Decorators

  implicit def TreeDeco(tree: Tree): TreeAPI
  implicit def PackageClauseDeco(pack: PackageClause): PackageClauseAPI
  implicit def ImportDeco(imp: Import): ImportAPI
  implicit def DefinitionDeco(definition: Definition): DefinitionAPI
  implicit def ClassDefDeco(cdef: ClassDef): ClassDefAPI
  implicit def DefDefDeco(ddef: DefDef): DefDefAPI
  implicit def ValDefDeco(vdef: ValDef): ValDefAPI
  implicit def TypeDefDeco(tdef: TypeDef): TypeDefAPI
  implicit def PackageDefDeco(pdef: PackageDef): PackageDefAPI
  implicit def TermDeco(term: Term): TermAPI
  implicit def IdentDeco(ident: Term.Ident): Term.IdentAPI
  implicit def SelectDeco(select: Term.Select): Term.SelectAPI
  implicit def LiteralDeco(x: Term.Literal): Term.LiteralAPI
  implicit def ThisDeco(x: Term.This): Term.ThisAPI
  implicit def NewDeco(x: Term.New): Term.NewAPI
  implicit def NamedArgDeco(x: Term.NamedArg): Term.NamedArgAPI
  implicit def ApplyDeco(x: Term.Apply): Term.ApplyAPI
  implicit def TypeApplyDeco(x: Term.TypeApply): Term.TypeApplyAPI
  implicit def SuperDeco(x: Term.Super): Term.SuperAPI
  implicit def TypedDeco(x: Term.Typed): Term.TypedAPI
  implicit def AssignDeco(x: Term.Assign): Term.AssignAPI
  implicit def BlockDeco(x: Term.Block): Term.BlockAPI
  implicit def LambdaDeco(x: Term.Lambda): Term.LambdaAPI
  implicit def IfDeco(x: Term.If): Term.IfAPI
  implicit def MatchDeco(x: Term.Match): Term.MatchAPI
  implicit def TryDeco(x: Term.Try): Term.TryAPI
  implicit def ReturnDeco(x: Term.Return): Term.ReturnAPI
  implicit def RepeatedDeco(x: Term.Repeated): Term.RepeatedAPI
  implicit def InlinedDeco(x: Term.Inlined): Term.InlinedAPI
  implicit def SelectOuterDeco(x: Term.SelectOuter): Term.SelectOuterAPI
  implicit def WhileDeco(x: Term.While): Term.WhileAPI

  implicit def termAsTermOrTypeTree(term: Term): TermOrTypeTree

  // ----- Tree -----------------------------------------------------

  trait TreeAPI {
    /** Position in the source code */
    def pos(implicit ctx: Context): Position

    def symbol(implicit ctx: Context): Symbol
  }

  val IsPackageClause: IsPackageClauseModule
  abstract class IsPackageClauseModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[PackageClause]
  }

  val PackageClause: PackageClauseModule
  abstract class PackageClauseModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[Tree])]
  }

  trait PackageClauseAPI {
  }

  // ----- Statements -----------------------------------------------

  val Import: ImportModule
  abstract class ImportModule {
    def unapply(imp: Tree)(implicit ctx: Context): Option[(Term, List[ImportSelector])]
  }

  trait ImportAPI {
    def expr(implicit ctx: Context): Term
    def selector(implicit ctx: Context): List[ImportSelector]
  }

  // ----- Definitions ----------------------------------------------

  val IsDefinition: IsDefinitionModule
  abstract class IsDefinitionModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[Definition]
  }

  trait DefinitionAPI {
    def name(implicit ctx: Context): String
  }

  // ClassDef

  val IsClassDef: IsClassDefModule
  abstract class IsClassDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[ClassDef]
  }

  val ClassDef: ClassDefModule
  abstract class ClassDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, DefDef, List[TermOrTypeTree], Option[ValDef], List[Statement])]
  }

  trait ClassDefAPI {
    def constructor(implicit ctx: Context): DefDef
    def parents(implicit ctx: Context): List[TermOrTypeTree]
    def self(implicit ctx: Context): Option[ValDef]
    def body(implicit ctx: Context): List[Statement]

    def symbol(implicit ctx: Context): ClassSymbol
  }

  // DefDef

  val IsDefDef: IsDefDefModule
  abstract class IsDefDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[DefDef]
  }

  val DefDef: DefDefModule
  abstract class DefDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, List[TypeDef],  List[List[ValDef]], TypeTree, Option[Term])]
  }

  trait DefDefAPI {
    def typeParams(implicit ctx: Context): List[TypeDef]
    def paramss(implicit ctx: Context): List[List[ValDef]]
    def returnTpt(implicit ctx: Context): TypeTree
    def rhs(implicit ctx: Context): Option[Term]

    def symbol(implicit ctx: Context): DefSymbol
  }

  // ValDef

  val IsValDef: IsValDefModule
  abstract class IsValDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[ValDef]
  }

  val ValDef: ValDefModule
  abstract class ValDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, TypeTree, Option[Term])]
  }

  trait ValDefAPI {
    def tpt(implicit ctx: Context): TypeTree
    def rhs(implicit ctx: Context): Option[Term]

    def symbol(implicit ctx: Context): ValSymbol
  }

  // TypeDef

  val IsTypeDef: IsTypeDefModule
  abstract class IsTypeDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[TypeDef]
  }

  val TypeDef: TypeDefModule
  abstract class TypeDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, TypeOrBoundsTree /* TypeTree | TypeBoundsTree */)]
  }

  trait TypeDefAPI {
    def rhs(implicit ctx: Context): TypeOrBoundsTree
    def symbol(implicit ctx: Context): TypeSymbol
  }

  // PackageDef

  val IsPackageDef: IsPackageDefModule
  abstract class IsPackageDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[PackageDef]
  }

  trait PackageDefAPI {
    def owner(implicit ctx: Context): PackageDef
    def members(implicit ctx: Context): List[Statement]
    def symbol(implicit ctx: Context): PackageSymbol
  }

  val PackageDef: PackageDefModule
  abstract class PackageDefModule {
    def unapply(tree: Tree)(implicit ctx: Context): Option[(String, PackageDef)]
  }

  // ----- Terms ----------------------------------------------------

  trait TermAPI {
    def tpe(implicit ctx: Context): Type
    def pos(implicit ctx: Context): Position
    def underlyingArgument(implicit ctx: Context): Term
    def underlying(implicit ctx: Context): Term
  }

  val IsTerm: IsTermModule
  abstract class IsTermModule {
    /** Matches any term */
    def unapply(tree: Tree)(implicit ctx: Context): Option[Term]
    /** Matches any term */
    def unapply(parent: TermOrTypeTree)(implicit ctx: Context, dummy: DummyImplicit): Option[Term]
  }

  /** Scala term. Any tree that can go in expression position. */
  val Term: TermModule
  abstract class TermModule extends TermCoreModule {

    val IsIdent: IsIdentModule
    abstract class IsIdentModule {
      /** Matches any Ident and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Ident]
    }

    trait IdentAPI {
      def name(implicit ctx: Context): String
    }

    /** Scala term identifier */
    val Ident: IdentModule
    abstract class IdentModule {
      /** Matches a term identifier and returns its name */
      def unapply(tree: Tree)(implicit ctx: Context): Option[String]
    }

    val IsSelect: IsSelectModule
    abstract class IsSelectModule {
      /** Matches any Select and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Select]
    }

    trait SelectAPI {
      def qualifier(implicit ctx: Context): Term
      def name(implicit ctx: Context): String
      def signature(implicit ctx: Context): Option[Signature]
    }

    /** Scala term selection */
    val Select: SelectModule
    abstract class SelectModule {
      /** Matches `<qual: Term>.<name: String>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, String)]
    }

    val IsLiteral: IsLiteralModule
    abstract class IsLiteralModule {
      /** Matches any Literal and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Literal]
    }

    trait LiteralAPI {
      def constant(implicit ctx: Context): Constant
    }

    /** Scala literal constant */
    val Literal: LiteralModule
    abstract class LiteralModule {
      def unapply(tree: Tree)(implicit ctx: Context): Option[Constant]
    }

    val IsThis: IsThisModule
    abstract class IsThisModule {
      /** Matches any This and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[This]
    }

    trait ThisAPI {
      def id(implicit ctx: Context): Option[Id]
    }

    /** Scala `this` or `this[id]` */
    val This: ThisModule
    abstract class ThisModule {
      /** Matches new `this[<id: Option[Id]>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Option[Id]]
    }

    val IsNew: IsNewModule
    abstract class IsNewModule {
      /** Matches any New and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[New]
    }

    trait NewAPI {
      def tpt(implicit ctx: Context): TypeTree
    }

    /** Scala `new` */
    val New: NewModule
    abstract class NewModule {
      /** Matches new `new <tpt: TypeTree>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[TypeTree]
    }

    val IsNamedArg: IsNamedArgModule
    abstract class IsNamedArgModule {
      /** Matches any NamedArg and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[NamedArg]
    }

    trait NamedArgAPI {
      def name(implicit ctx: Context): String
      def value(implicit ctx: Context): Term
    }

    /** Scala named argument `x = y` in argument position */
    val NamedArg: NamedArgModule
    abstract class NamedArgModule {
      /** Matches `<name: String> = <value: Term>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(String, Term)]
    }

    val IsApply: IsApplyModule
    abstract class IsApplyModule {
      /** Matches any Apply and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Apply]
    }

    trait ApplyAPI {
      def fun(implicit ctx: Context): Term
      def args(implicit ctx: Context): List[Term]
    }

    /** Scala parameter application */
    val Apply: ApplyModule
    abstract class ApplyModule {
      /** Matches function application `<fun: Term>(<args: List[Term]>)` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[Term])]
    }

    val IsTypeApply: IsTypeApplyModule
    abstract class IsTypeApplyModule {
      /** Matches any TypeApply and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[TypeApply]
    }

    trait TypeApplyAPI {
      def fun(implicit ctx: Context): Term
      def args(implicit ctx: Context): List[TypeTree]
    }

    /** Scala type parameter application */
    val TypeApply: TypeApplyModule
    abstract class TypeApplyModule {
      /** Matches function type application `<fun: Term>[<args: List[TypeTree]>]` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[TypeTree])]
    }

    val IsSuper: IsSuperModule
    abstract class IsSuperModule {
      /** Matches any Super and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Super]
    }

    trait SuperAPI {
      def qualifier(implicit ctx: Context): Term
      def id(implicit ctx: Context): Option[Id]
    }

    /** Scala `x.super` or `x.super[id]` */
    val Super: SuperModule
    abstract class SuperModule {
      /** Matches new `<qualifier: Term>.super[<id: Option[Id]>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Option[Id])]
    }

    val IsTyped: IsTypedModule
    abstract class IsTypedModule {
      /** Matches any Typed and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Typed]
    }

    trait TypedAPI {
      def expr(implicit ctx: Context): Term
      def tpt(implicit ctx: Context): Term
    }

    /** Scala ascription `x: T` */
    val Typed: TypedModule
    abstract class TypedModule {
      /** Matches `<expr: Term>: <tpt: Term>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, TypeTree)]
    }

    val IsAssign: IsAssignModule
    abstract class IsAssignModule {
      /** Matches any Assign and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Assign]
    }

    trait AssignAPI {
      def lhs(implicit ctx: Context): Term
      def rhs(implicit ctx: Context): Term
    }

    /** Scala assign `x = y` */
    val Assign: AssignModule
    abstract class AssignModule {
      /** Matches `<lhs: Term> = <rhs: Term>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Term)]
    }

    val IsBlock: IsBlockModule
    abstract class IsBlockModule {
      /** Matches any Block and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Block]
    }

    trait BlockAPI {
      def statements(implicit ctx: Context): List[Statement]
      def expr(implicit ctx: Context): Term
    }

    /** Scala code block `{ stat0; ...; statN; expr }` term */
    val Block: BlockModule
    abstract class BlockModule {
      /** Matches `{ <statements: List[Statement]>; <expr: Term> }` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(List[Statement], Term)]
    }

    val IsLambda: IsLambdaModule
    abstract class IsLambdaModule {
      /** Matches any Lambda and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Lambda]
    }

    trait LambdaAPI {
      def meth(implicit ctx: Context): Term
      def tptOpt(implicit ctx: Context): Option[TypeTree]
    }

    val Lambda: LambdaModule
    abstract class LambdaModule {
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Option[TypeTree])]
    }

    val IsIf: IsIfModule
    abstract class IsIfModule {
      /** Matches any If and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[If]
    }

    trait IfAPI {
      def cond(implicit ctx: Context): Term
      def thenp(implicit ctx: Context): Term
      def elsep(implicit ctx: Context): Term
    }

    /** Scala `if`/`else` term */
    val If: IfModule
    abstract class IfModule {
      /** Matches `if (<cond: Term>) <thenp: Term> else <elsep: Term>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Term, Term)]
    }

    val IsMatch: IsMatchModule
    abstract class IsMatchModule {
      /** Matches any Match and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Match]
    }

    trait MatchAPI {
      def scrutinee(implicit ctx: Context): Term
      def cases(implicit ctx: Context): List[CaseDef]
    }

    /** Scala `match` term */
    val Match: MatchModule
    abstract class MatchModule {
      /** Matches `<scrutinee: Trem> match { <cases: List[CaseDef]> }` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[CaseDef])]
    }

    val IsTry: IsTryModule
    abstract class IsTryModule {
      /** Matches any Try and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Try]
    }

    trait TryAPI {
      def body(implicit ctx: Context): Term
      def cases(implicit ctx: Context): List[CaseDef]
      def finalizer(implicit ctx: Context): Option[Term]
    }

    /** Scala `try`/`catch`/`finally` term */
    val Try: TryModule
    abstract class TryModule {
      /** Matches `try <body: Term> catch { <cases: List[CaseDef]> } finally <finalizer: Option[Term]>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, List[CaseDef], Option[Term])]
    }

    val IsReturn: IsReturnModule
    abstract class IsReturnModule {
      /** Matches any Return and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Return]
    }

    trait ReturnAPI {
      def expr(implicit ctx: Context): Term
    }

    /** Scala local `return` */
    val Return: ReturnModule
    abstract class ReturnModule {
      /** Matches `return <expr: Term>` */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Term]
    }

    val IsRepeated: IsRepeatedModule
    abstract class IsRepeatedModule {
      /** Matches any Repeated and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Repeated]
    }

    trait RepeatedAPI {
      def elems(implicit ctx: Context): List[Term]
    }

    val Repeated: RepeatedModule
    abstract class RepeatedModule {
      def unapply(tree: Tree)(implicit ctx: Context): Option[List[Term]]
    }

    val IsInlined: IsInlinedModule
    abstract class IsInlinedModule {
      /** Matches any Inlined and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[Inlined]
    }

    trait InlinedAPI {
      def call(implicit ctx: Context): Option[TermOrTypeTree]
      def bindings(implicit ctx: Context): List[Definition]
      def body(implicit ctx: Context): Term
    }

    val Inlined: InlinedModule
    abstract class InlinedModule {
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Option[TermOrTypeTree], List[Definition], Term)]
    }

    val IsSelectOuter: IsSelectOuterModule
    abstract class IsSelectOuterModule {
      /** Matches any SelectOuter and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[SelectOuter]
    }

    trait SelectOuterAPI {
      def qualifier(implicit ctx: Context): Term
      def level(implicit ctx: Context): Int
      def tpe(implicit ctx: Context): Type
    }

    val SelectOuter: SelectOuterModule
    abstract class SelectOuterModule {
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Int, Type)]
    }

    val IsWhile: IsWhileModule
    abstract class IsWhileModule {
      /** Matches any While and returns it */
      def unapply(tree: Tree)(implicit ctx: Context): Option[While]
    }

    trait WhileAPI {
      def cond(implicit ctx: Context): Term
      def body(implicit ctx: Context): Term
    }

    val While: WhileModule
    abstract class WhileModule {
      /** Extractor for while loops. Matches `while (<cond>) <body>` and returns (<cond>, <body>) */
      def unapply(tree: Tree)(implicit ctx: Context): Option[(Term, Term)]
    }
  }

}
