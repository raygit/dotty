import quoted._
import dotty.tools.dotc.quoted.Toolbox._

object Test {
  def main(args: Array[String]): Unit = {
    val q = '{ '(3) }
    println(q.show)
  }
}
