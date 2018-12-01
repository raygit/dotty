package dotty.tools.dotc
package core
package tasty

import Contexts._, Decorators._
import Names.Name
import TastyUnpickler._
import TastyBuffer.NameRef
import util.Positions.offsetToInt
import printing.Highlighting._

class TastyPrinter(bytes: Array[Byte])(implicit ctx: Context) {

  val unpickler: TastyUnpickler = new TastyUnpickler(bytes)
  import unpickler.{nameAtRef, unpickle}

  def nameToString(name: Name): String = name.debugString

  def nameRefToString(ref: NameRef): String = nameToString(nameAtRef(ref))

  def printNames(): Unit =
    for ((name, idx) <- nameAtRef.contents.zipWithIndex) {
      val index = nameColor("%4d".format(idx))
      println(index + ": " + nameToString(name))
    }

  def printContents(): Unit = {
    println("Names:")
    printNames()
    println()
    println("Trees:")
    unpickle(new TreeSectionUnpickler)
    unpickle(new PositionSectionUnpickler)
    unpickle(new CommentSectionUnpickler)
  }

  class TreeSectionUnpickler extends SectionUnpickler[Unit](TreePickler.sectionName) {
    import TastyFormat._
    def unpickle(reader: TastyReader, tastyName: NameTable): Unit = {
      import reader._
      var indent = 0
      def newLine() = {
        val length = treeColor("%5d".format(index(currentAddr) - index(startAddr)))
        print(s"\n $length:" + " " * indent)
      }
      def printNat() = print(Yellow(" " + readNat()).show)
      def printName() = {
        val idx = readNat()
        print(nameColor(" " + idx + " [" + nameRefToString(NameRef(idx)) + "]"))
      }
      def printTree(): Unit = {
        newLine()
        val tag = readByte()
        print(" ");print(astTagToString(tag))
        indent += 2
        if (tag >= firstLengthTreeTag) {
          val len = readNat()
          print(s"(${lengthColor(len.toString)})")
          val end = currentAddr + len
          def printTrees() = until(end)(printTree())
          tag match {
            case RENAMED =>
              printName(); printName()
            case VALDEF | DEFDEF | TYPEDEF | OBJECTDEF | TYPEPARAM | PARAM | NAMEDARG | BIND =>
              printName(); printTrees()
            case REFINEDtype | TERMREFin | TYPEREFin =>
              printName(); printTree(); printTrees()
            case RETURN | HOLE =>
              printNat(); printTrees()
            case METHODtype | IMPLICITMETHODtype | ERASEDMETHODtype | ERASEDIMPLICITMETHODtype | POLYtype | TYPELAMBDAtype =>
              printTree()
              until(end) { printName(); printTree() }
            case PARAMtype =>
              printNat(); printNat()
            case _ =>
              printTrees()
          }
          if (currentAddr != end) {
            println(s"incomplete read, current = $currentAddr, end = $end")
            goto(end)
          }
        }
        else if (tag >= firstNatASTTreeTag) {
          tag match {
            case IDENT | IDENTtpt | SELECT | SELECTtpt | TERMREF | TYPEREF | SELFDEF => printName()
            case _ => printNat()
          }
          printTree()
        }
        else if (tag >= firstASTTreeTag)
          printTree()
        else if (tag >= firstNatTreeTag)
          tag match {
            case TERMREFpkg | TYPEREFpkg | STRINGconst | IMPORTED => printName()
            case _ => printNat()
          }
        indent -= 2
      }
      println(i"start = ${reader.startAddr}, base = $base, current = $currentAddr, end = $endAddr")
      println(s"${endAddr.index - startAddr.index} bytes of AST, base = $currentAddr")
      while (!isAtEnd) {
        printTree()
        newLine()
      }
    }
  }

  class PositionSectionUnpickler extends SectionUnpickler[Unit]("Positions") {
    def unpickle(reader: TastyReader, tastyName: NameTable): Unit = {
      print(s" ${reader.endAddr.index - reader.currentAddr.index}")
      val positions = new PositionUnpickler(reader).positions
      println(s" position bytes:")
      val sorted = positions.toSeq.sortBy(_._1.index)
      for ((addr, pos) <- sorted) {
        print(treeColor("%10d".format(addr.index)))
        println(s": ${offsetToInt(pos.start)} .. ${pos.end}")
      }
    }
  }

  class CommentSectionUnpickler extends SectionUnpickler[Unit]("Comments") {
    def unpickle(reader: TastyReader, tastyName: NameTable): Unit = {
      print(s" ${reader.endAddr.index - reader.currentAddr.index}")
      val comments = new CommentUnpickler(reader).comments
      println(s" comment bytes:")
      val sorted = comments.toSeq.sortBy(_._1.index)
      for ((addr, cmt) <- sorted) {
        print(treeColor("%10d".format(addr.index)))
        println(s": ${cmt.raw} (expanded = ${cmt.isExpanded})")
      }
    }
  }

  private def nameColor(str: String): String = Magenta(str).show
  private def treeColor(str: String): String = Yellow(str).show
  private def lengthColor(str: String): String = Cyan(str).show
}
