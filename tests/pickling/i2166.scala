object Test {
  transparent def f = "" match { case _ => false }

  def main(args: Array[String]): Unit = f
}