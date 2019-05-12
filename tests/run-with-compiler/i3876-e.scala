import scala.quoted._
object Test {
  def main(args: Array[String]): Unit = {
    implicit val toolbox: scala.quoted.Toolbox = scala.quoted.Toolbox.make(getClass.getClassLoader)

    val x: Expr[Int] = '{ println(); 3 }

    val f4: Expr[Int => Int] = '{
      inlineLambda
    }
    println(f4(x).run)
    println(f4(x).show)
  }

  inline def inlineLambda <: Int => Int = x => x + x
}