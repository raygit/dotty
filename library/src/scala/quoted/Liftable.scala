package scala.quoted

/** A typeclass for types that can be turned to `quoted.Expr[T]`
 *  without going through an explicit `'(...)` operation.
 */
abstract class Liftable[T] {
  implicit def toExpr(x: T): Expr[T]
}

/** Some liftable base types. To be completed with at least all types
 *  that are valid Scala literals. The actual implementation of these
 *  typed could be in terms of `ast.tpd.Literal`; the test `quotable.scala`
 *  gives an alternative implementation using just the basic staging system.
 */
object Liftable {

  implicit class LiftExprOps[T](val x: T) extends AnyVal {
    def toExpr(implicit liftable: Liftable[T]): Expr[T] = liftable.toExpr(x)
  }

  final class ConstantExpr[T] private[Liftable](val value: T) extends Expr[T] {
    override def toString: String = s"Expr($value)"
  }

  implicit def BooleanIsLiftable: Liftable[Boolean] = (x: Boolean) => new ConstantExpr(x)
  implicit def ByteLiftable: Liftable[Byte] = (x: Byte) => new ConstantExpr(x)
  implicit def CharIsLiftable: Liftable[Char] = (x: Char) => new ConstantExpr(x)
  implicit def ShortIsLiftable: Liftable[Short] = (x: Short) => new ConstantExpr(x)
  implicit def IntIsLiftable: Liftable[Int] = (x: Int) => new ConstantExpr(x)
  implicit def LongIsLiftable: Liftable[Long] = (x: Long) => new ConstantExpr(x)
  implicit def FloatIsLiftable: Liftable[Float] = (x: Float) => new ConstantExpr(x)
  implicit def DoubleIsLiftable: Liftable[Double] = (x: Double) => new ConstantExpr(x)

  implicit def StringIsLiftable: Liftable[String] = (x: String) => new ConstantExpr(x)
}
