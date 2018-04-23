package scala

import annotation.implicitNotFound
import scala.collection.{GenSeq, Set}

/** A marker trait indicating that values of type `L` can be compared to values of type `R`. */
@implicitNotFound("Values of types ${L} and ${R} cannot be compared with == or !=")
sealed trait Eq[-L, -R]

/** Besides being a companion object, this object
 *  can also be used as a value that's compatible with
 *  any instance of `Eq`.
 */
object Eq extends Eq[Any, Any] {

  /** A fall-back "implicit" to compare values of any types.
   *  Even though this method is not declared implicit, the compiler will
   *  compute instances as solutions to `Eq[T, U]` queries if `T <: U` or `U <: T`
   *  or both `T` and `U` are Eq-free. A type `S` is Eq-free if there is no
   *  implicit instance of type `Eq[S, S]`.
   */
  def eqAny[L, R]: Eq[L, R] = Eq

  // Instances of `Eq` for common types

  implicit def eqNumber   : Eq[Number, Number] = Eq
  implicit def eqString   : Eq[String, String] = Eq
  implicit def eqBoolean  : Eq[Boolean, Boolean] = Eq
  implicit def eqByte     : Eq[Byte, Byte] = Eq
  implicit def eqShort    : Eq[Short, Short] = Eq
  implicit def eqChar     : Eq[Char, Char] = Eq
  implicit def eqInt      : Eq[Int, Int] = Eq
  implicit def eqLong     : Eq[Long, Long] = Eq
  implicit def eqFloat    : Eq[Float, Float] = Eq
  implicit def eqDouble   : Eq[Double, Double] = Eq
  implicit def eqUnit     : Eq[Unit, Unit] = Eq

  // true asymmetry, modeling the (somewhat problematic) nature of equals on Proxies
  implicit def eqProxy    : Eq[Proxy, Any]     = Eq

  implicit def eqSeq[T, U](implicit eq: Eq[T, U]): Eq[GenSeq[T], GenSeq[U]] = Eq
  implicit def eqSet[T, U](implicit eq: Eq[T, U]): Eq[Set[T], Set[U]] = Eq

  implicit def eqByteNum  : Eq[Byte, Number]   = Eq
  implicit def eqNumByte  : Eq[Number, Byte]   = Eq
  implicit def eqCharNum  : Eq[Char, Number]   = Eq
  implicit def eqNumChar  : Eq[Number, Char]   = Eq
  implicit def eqShortNum : Eq[Short, Number]  = Eq
  implicit def eqNumShort : Eq[Number, Short]  = Eq
  implicit def eqIntNum   : Eq[Int, Number]    = Eq
  implicit def eqNumInt   : Eq[Number, Int]    = Eq
  implicit def eqLongNum  : Eq[Long, Number]   = Eq
  implicit def eqNumLong  : Eq[Number, Long]   = Eq
  implicit def eqFloatNum : Eq[Float, Number]  = Eq
  implicit def eqNumFloat : Eq[Number, Float]  = Eq
  implicit def eqDoubleNum: Eq[Double, Number] = Eq
  implicit def eqNumDouble: Eq[Number, Double] = Eq

  implicit def eqSBoolJBool: Eq[Boolean, java.lang.Boolean] = Eq
  implicit def eqJBoolSBool: Eq[java.lang.Boolean, Boolean] = Eq
}
