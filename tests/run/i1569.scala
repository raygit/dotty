object Test {
  transparent def foo(transparent n: => Int) = n + n

  def main(args: Array[String]): Unit = foo({ println("foo"); 42 })
}
