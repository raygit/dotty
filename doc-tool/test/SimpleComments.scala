package dotty.tools
package dottydoc

import model.internal._

import org.junit.Test
import org.junit.Assert._

class TestSimpleComments extends DottyDocTest {

  @Test def cookCommentEmptyClass = {
    val source =
      """
      |package scala
      |
      |/**
      | * An empty trait: $Variable
      | *
      | * @define Variable foobar
      | */
      |trait Test""".stripMargin

    checkSource(source) { packages =>
      packages("scala") match {
        case PackageImpl(_, _, _, List(trt), _, _, _, _) =>
          assert(trt.comment.isDefined, "Lost comment in transformations")
          assert(trt.comment.get.body.contains("An empty trait: foobar"))
          assert(trt.name == "Test", s"Incorrect name after transform: ${trt.name}")
      }
    }
  }

  @Test def simpleComment = {
    val source =
      """
      |package scala
      |
      |/** Hello, world! */
      |trait HelloWorld
      """.stripMargin

    checkSource(source) { packages =>
      val traitCmt =
        packages("scala")
        .children.find(_.path.mkString(".") == "scala.HelloWorld")
        .flatMap(_.comment.map(_.body))
        .get

      assertEquals(traitCmt, "<p>Hello, world!</p>")
    }
  }

  @Test def commentOnPackageObject = {
    val source =
      """
      |/** Hello, world! */
      |package object foobar { class A }
      """.stripMargin

    checkSource(source) { packages =>
      val packageCmt = packages("foobar").comment.get.body
      assertEquals("<p>Hello, world!</p>", packageCmt)
    }
  }
}
