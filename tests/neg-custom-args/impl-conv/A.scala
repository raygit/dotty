package implConv

object A {

  implicit def s2i(x: String): Int = Integer.parseInt(x) // error: feature

  implicit class Foo(x: String) {
    def foo = x.length
  }

}
