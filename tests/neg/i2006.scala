object Test {

  transparent def foo(f: ImplicitFunction1[Int, Int]): AnyRef = f // error
  transparent def bar(f: ImplicitFunction1[Int, Int]) = f // error

  def main(args: Array[String]) = {
    foo(implicit thisTransaction => 43)
    bar(implicit thisTransaction => 44)
  }
}
