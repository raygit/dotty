package scala.tasty.reflect

/** Tasty reflect abstract types
 *
 *  ```none
 *
 *  +- Tree -+- PackageClause
 *           +- Import
 *           +- Statement -+- Definition --+- PackageDef
 *                         |               +- ClassDef
 *                         |               +- TypeDef
 *                         |               +- DefDef
 *                         |               +- ValDef
 *                         |
 *                         +- Term --------+- Ident
 *                                         +- Select
 *                                         +- Literal
 *                                         +- This
 *                                         +- New
 *                                         +- NamedArg
 *                                         +- Apply
 *                                         +- TypeApply
 *                                         +- Super
 *                                         +- Typed
 *                                         +- Assign
 *                                         +- Block
 *                                         +- Lambda
 *                                         +- If
 *                                         +- Match
 *                                         +- Try
 *                                         +- Return
 *                                         +- Repeated
 *                                         +- Inlined
 *                                         +- SelectOuter
 *                                         +- While
 *                                         +- DoWhile
 *
 *
 *                         +- TypeTree ----+- Synthetic
 *                         |               +- Ident
 *                         |               +- Select
 *                         |               +- Project
 *                         |               +- Singleton
 *  +- TypeOrBoundsTree ---+               +- Refined
 *                         |               +- Applied
 *                         |               +- Annotated
 *                         |               +- And
 *                         |               +- Or
 *                         |               +- ByName
 *                         |               +- TypeLambdaTree
 *                         |               +- Bind
 *                         |
 *                         +- TypeBoundsTree
 *                         +- SyntheticBounds
 *
 *  +- CaseDef
 *
 *  +- Pattern --+- Value
 *               +- Bind
 *               +- Unapply
 *               +- Alternative
 *               +- TypeTest
 *
 *
 *                   +- NoPrefix
 *  +- TypeOrBounds -+- TypeBounds
 *                   |
 *                   +- Type -------+- ConstantType
 *                                  +- SymRef
 *                                  +- TermRef
 *                                  +- TypeRef
 *                                  +- SuperType
 *                                  +- Refinement
 *                                  +- AppliedType
 *                                  +- AnnotatedType
 *                                  +- AndType
 *                                  +- OrType
 *                                  +- ByNameType
 *                                  +- ParamRef
 *                                  +- ThisType
 *                                  +- RecursiveThis
 *                                  +- RecursiveType
 *                                  +- LambdaType[ParamInfo <: TypeOrBounds] -+- MethodType
 *                                                                            +- PolyType
 *                                                                            +- TypeLambda
 *
 *  +- ImportSelector -+- SimpleSelector
 *                     +- RenameSelector
 *                     +- OmitSelector
 *
 *  +- Id
 *
 *  +- Signature
 *
 *  +- Position
 *
 *  +- Constant
 *
 *  +- Symbol --+- PackageSymbol
 *              +- ClassSymbol
 *              +- TypeSymbol
 *              +- DefSymbol
 *              +- ValSymbol
 *              +- BindSymbol
 *              +- NoSymbol
 *
 *  Aliases:
 *   # TermOrTypeTree = Term | TypeTree
 *
 *  ```
 */
trait TastyCore {

  /** Compilation context */
  type Context

  /** Settings */
  type Settings

  // TODO: When bootstrapped, remove and use `Term | TypeTree` type directly in other files
  /** Workaround missing `|` types in Scala 2 to represent `Term | TypeTree` */
  type TermOrTypeTree /* Term | TypeTree */

  /** Tree representing executable code written in the source */
  type Tree
    type PackageClause <: Tree
    type Statement <: Tree
      type Import <: Statement
      type Definition <: Statement
        type PackageDef <: Definition
        type ClassDef <: Definition
        type TypeDef <: Definition
        type DefDef <: Definition
        type ValDef <: Definition
      type Term <: Statement

  /** Branch of a pattern match or catch clause */
  type CaseDef

  /** Pattern tree of the pattern part of a CaseDef */
  type Pattern
    type Value <: Pattern
    type Bind <: Pattern
    type Unapply <: Pattern
    type Alternative <: Pattern
    type TypeTest <: Pattern

  /** Tree representing a type written in the source */
  type TypeOrBoundsTree
    type TypeTree <: TypeOrBoundsTree
    type TypeBoundsTree <: TypeOrBoundsTree

  type TypeOrBounds
    type NoPrefix <: TypeOrBounds
    type TypeBounds <: TypeOrBounds
    type Type <: TypeOrBounds
    type RecursiveType <: Type
    // TODO can we add the bound back without an cake?
    // TODO is LambdaType really needed? ParamRefExtractor could be split into more precise extractors
    type LambdaType[ParamInfo /*<: TypeOrBounds*/] <: Type
      type MethodType <: LambdaType[Type]
      type PolyType <: LambdaType[TypeBounds]
      type TypeLambda <: LambdaType[TypeBounds]


  type ImportSelector

  /** Untyped identifier */
  type Id

  /** JVM signature of a method */
  type Signature

  /** Source position */
  type Position

  /** Constant value represented as the constant itself */
  type Constant

  /** Symbol of a definition.
   *  Then can be compared with == to know if the definition is the same.
   */
  type Symbol
    type PackageSymbol <: Symbol
    type ClassSymbol <: Symbol
    type TypeSymbol <: Symbol
    type DefSymbol <: Symbol
    type ValSymbol <: Symbol
    type BindSymbol <: Symbol
    type NoSymbol <: Symbol

}
