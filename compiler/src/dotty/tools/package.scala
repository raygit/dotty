package dotty
import scala.annotation.Annotation

package object tools {
  // Ensure this object is already classloaded, since it's only actually used
  // when handling stack overflows and every operation (including class loading)
  // risks failing.
  dotty.tools.dotc.core.handleRecursive

  val ListOfNil = Nil :: Nil

  /** True if two lists have the same length.  Since calling length on linear sequences
   *  is O(n), it is an inadvisable way to test length equality.
   */
  final def sameLength[T](xs: List[T], ys: List[T]): Boolean = xs match {
    case _ :: xs1 =>
      ys match {
        case _ :: ys1 => sameLength(xs1, ys1)
        case _ => false
      }
    case _ => ys.isEmpty
  }
}
