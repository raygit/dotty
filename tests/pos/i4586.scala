class Foo {
  transparent def foo1(f: => Int => Int): Int = f(7)
  def bar1 = foo1(x => x + 1)
}
