package dotty.tools.dotc.quoted

import dotty.tools.dotc.ast.Trees._
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Constants._
import dotty.tools.dotc.printing.RefinedPrinter

import scala.quoted.Expr
import scala.runtime.BoxedUnit
import scala.quoted.Exprs.ValueExpr
import scala.runtime.quoted._

/** Default runners for quoted expressions */
object Runners {
  import tpd._

  type Run
  type Show

  implicit def runner[T]: Runner[T] = new Runner[T] {

    def run(expr: Expr[T]): T = Runners.run(expr, Settings.run())

    def show(expr: Expr[T]): String = Runners.show(expr, Settings.show())

    def toConstantOpt(expr: Expr[T]): Option[T] = {
      def toConstantOpt(tree: Tree): Option[T] = tree match {
        case Literal(Constant(c)) => Some(c.asInstanceOf[T])
        case Block(Nil, e) => toConstantOpt(e)
        case Inlined(_, Nil, e) => toConstantOpt(e)
        case _ => None
      }
      expr match {
        case expr: ValueExpr[T] => Some(expr.value)
        case _ => new QuoteDriver().withTree(expr, (tree, _) => toConstantOpt(tree), Settings.run())
      }
    }

  }

  def run[T](expr: Expr[T], settings: Settings[Run]): T = expr match {
    case expr: ValueExpr[T] => expr.value
    case _ => new QuoteDriver().run(expr, settings)
  }

  def show[T](expr: Expr[T], settings: Settings[Show]): String = expr match {
    case expr: ValueExpr[T] =>
      implicit val ctx = new QuoteDriver().initCtx
      if (settings.compilerArgs.contains("-color:never"))
        ctx.settings.color.update("never")
      val printer = new RefinedPrinter(ctx)
      if (expr.value == BoxedUnit.UNIT) "()"
      else printer.toText(Literal(Constant(expr.value))).mkString(Int.MaxValue, false)
    case _ => new QuoteDriver().show(expr, settings)
  }

  class Settings[T] private (val outDir: Option[String], val compilerArgs: List[String])

  object Settings {

    /** Quote run settings
     *  @param optimise Enable optimisation when compiling the quoted code
     *  @param outDir Output directory for the compiled quote. If set to None the output will be in memory
     *  @param compilerArgs Compiler arguments. Use only if you know what you are doing.
     */
    def run(
        optimise: Boolean = false,
        outDir: Option[String] = None,
        compilerArgs: List[String] = Nil
        ): Settings[Run] = {
      var compilerArgs1 = compilerArgs
      if (optimise) compilerArgs1 = "-optimise" :: compilerArgs1
      new Settings(outDir, compilerArgs1)
    }

    /** Quote show settings
     *  @param compilerArgs Compiler arguments. Use only if you know what you are doing.
     */
    def show(
        color: Boolean = false,
        compilerArgs: List[String] = Nil
        ): Settings[Show] = {
      var compilerArgs1 = compilerArgs
      compilerArgs1 = s"-color:${if (color) "always" else "never"}" :: compilerArgs1
      new Settings(None, compilerArgs1)
    }

  }

}
