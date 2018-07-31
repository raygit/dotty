import scala.quoted._

import scala.tasty._

class LineNumber(val value: Int) {
  override def toString: String = value.toString
}

object LineNumber {

  implicit transparent def line: LineNumber =
    ~lineImpl(TopLevelSplice.tastyContext) // FIXME infer TopLevelSplice.tastyContext within top level ~

  def lineImpl(implicit tasty: Tasty): Expr[LineNumber] = {
    import tasty._
    '(new LineNumber(~rootPosition.startLine.toExpr))
  }

}
