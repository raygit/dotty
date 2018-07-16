import scala.quoted._

import dotty.tools.dotc.quoted.Toolbox._

import scala.tasty._

class LineNumber(val value: Int) {
  override def toString: String = value.toString
}

object LineNumber {

  implicit inline def line[T >: Unit <: Unit]: LineNumber =
    ~lineImpl('[T])(TopLevelSplice.tastyContext) // FIXME infer TopLevelSplice.tastyContext within top level ~

  def lineImpl(x: Type[Unit])(implicit tasty: Tasty): Expr[LineNumber] = {
    import tasty._

    '(new LineNumber(~x.toTasty.pos.startLine.toExpr))
  }

}
