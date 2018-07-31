package dotty.tools.dotc

import dotty.tools.dotc.ast.Trees.{Tree, Untyped}
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.Types.Type

package object tastyreflect {

  type PackageDefinition = PackageDefinitionImpl[Type]

  /** Represents the symbol of a definition in tree form */
  case class PackageDefinitionImpl[-T >: Untyped] private[tastyreflect] (sym: Symbol) extends Tree[T] {
    type ThisTree[-T >: Untyped] = PackageDefinitionImpl[T]
    override def denot(implicit ctx: Context) = sym.denot
  }

}
