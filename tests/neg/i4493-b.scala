class Index[K]
object Index {
  transparent def succ[K](x: K): Unit = ~{ // error
    implicit val t: quoted.Type[K] = '[K]
    '(new Index[K])
  }
}
