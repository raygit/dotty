import scala.quoted._
import dotty.tools.dotc.quoted.Toolbox._

import scala.tasty.Universe
import scala.tasty.util.TreeTraverser

object Macros {

  implicit inline def printType[T]: Unit =
    ~impl('[T])(Universe.compilationUniverse) // FIXME infer Universe.compilationUniverse within top level ~

  def impl[T](x: Type[T])(implicit u: Universe): Expr[Unit] = {
    import u._
    import u.tasty._
    val tree = x.toTasty
    '{
      println(~tree.show.toExpr)
      println(~tree.tpe.show.toExpr)
      println()
    }
  }
}
