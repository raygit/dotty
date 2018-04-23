package dotty

import org.junit.Test
import org.junit.Assert._

class ShowTests {
  import Show._

  @Test def showString = {
    assertEquals(""""\\"""", "\\".show)
    assertEquals("\"\\thello world!\"", "\thello world!".show)
    assertEquals("\"\\nhello world!\"", "\nhello world!".show)
    assertEquals("\"\\rhello world!\"", "\rhello world!".show)
    assertEquals(""""\b\t\n\f\r\'\"\\"""", "\b\t\n\f\r\'\"\\".show)
  }

  @Test def showFloat = {
    assertEquals("1.0f", 1.0f.show)
    assertEquals("1.0f", 1.0F.show)
  }

  @Test def showDouble = {
    assertEquals("1.0", 1.0d.show)
    assertEquals("1.0", 1.0.show)
  }

  @Test def showChar = {
    assertEquals("'\\b'", '\b'.show)
    assertEquals("'\\t'", '\t'.show)
    assertEquals("'\\n'", '\n'.show)
    assertEquals("'\\f'", '\f'.show)
    assertEquals("'\\r'", '\r'.show)
    assertEquals("'\\''", '\''.show)
    assertEquals("'\\\"'", '\"'.show)
    assertEquals("'\\\\'", '\\'.show)
  }

  @Test def showCar = {
    case class Car(model: String, manufacturer: String, year: Int)
    implicit val showCar: Show[Car] = new Show[Car] {
      def show(c: Car) =
        "Car(" + c.model.show + ", " + c.manufacturer.show + ", " + c.year.show + ")"
    }

    case class Shop(xs: List[Car], name: String)
    implicit val showShop: Show[Shop] = new Show[Shop] {
      def show(sh: Shop) =
        "Shop(" + sh.xs.show + ", " + sh.name.show + ")"
    }

    assertEquals("Car(\"Mustang\", \"Ford\", 1967)", Car("Mustang", "Ford", 1967).show)
  }

  @Test def showOptions = {
    assertEquals("None", None.show)
    val empty = Option.empty
    assertEquals("None", empty.show)
    assertEquals("None", (None: Option[String]).show)
    assertEquals("Some(\"hello opt\")", Some("hello opt").show)
  }

  @Test def showMaps = {
    val mp = scala.collection.immutable.Map("str1" -> "val1", "str2" -> "val2")
    assertEquals("Map(\"str1\" -> \"val1\", \"str2\" -> \"val2\")", mp.show)
  }

  @Test def withoutShow = {
    case class Car(model: String, manufacturer: String, year: Int)
    assertEquals("Car(Mustang,Ford,1967)", Car("Mustang", "Ford", 1967).show)
  }

  @Test def partialShow = {
    case object Foo
    assertEquals("Map(Foo -> \"Hello\")", Map(Foo -> "Hello").show)
  }

  @Test def showArrays = {
    assertEquals("Array()", Array[Int]().show)
    assertEquals("Array(1)", Array(1).show)
    assertEquals("Array(1, 2, 3)", Array(1, 2, 3).show)
  }

  @Test def showNull = {
    assertEquals("null", (null: String).show)
    assertEquals("List(null)", List(null).show)
    assertEquals("List(null)", List[String](null).show)
  }

  @Test def showNothing = {
    val emptyMap = Map()
    assertEquals("Map()", emptyMap.show)
    assertEquals("List()", List().show)
  }
}
