
trait HList {
  def length: Int = ???
  def head: Any
  def tail: HList

  inline def isEmpty: Boolean = length == 0
}

case object HNil extends HList {
  inline override def length = 0
  def head: Nothing = ???
  def tail: Nothing = ???
}

case class :: [H, T <: HList] (hd: H, tl: T) extends HList {
  inline override def length = 1 + tl.length
  def head: H = this.hd
  def tail: T = this.tl
}

object Test extends App {
  type HNil = HNil.type

  class HListDeco(val as: HList) extends AnyVal {
    inline def :: [H] (a: H): HList = new :: (a, as)
    inline def ++ (bs: HList): HList = concat(as, bs)
    inline def apply(idx: Int): Any = index(as, idx)
  }

  inline implicit def hlistDeco(xs: HList): HListDeco = new HListDeco(xs)

  inline def concat[T1, T2](xs: HList, ys: HList): HList =
    if xs.isEmpty then ys
    else new ::(xs.head, concat(xs.tail, ys))

  val xs = 1 :: "a" :: "b" :: HNil
  val ys = true :: 1.0 :: HNil
  val zs = concat(xs, ys)

  val control: Int :: String :: String :: Boolean :: Double :: HNil = zs

  inline def index(xs: HList, idx: Int): Any =
    if idx == 0 then xs.head
    else index(xs.tail, idx - 1)

  val zs0 = index(zs, 0)
  val zs1 = zs(1)
  val zs2 = zs(2)
  val zs3 = zs(3)
  def zs4 = zs(4)
}