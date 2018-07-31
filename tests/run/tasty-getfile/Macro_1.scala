import scala.quoted._
import scala.tasty.{Tasty, TopLevelSplice}

object SourceFiles {

  implicit transparent def getThisFile: String =
    ~getThisFileImpl(TopLevelSplice.tastyContext) // FIXME infer TopLevelSplice.tastyContext within top level ~

  private def getThisFileImpl(implicit tasty: Tasty): Expr[String] = {
    import tasty._
    rootContext.source.getFileName.toString.toExpr
  }

}
