package dotty.tools
package dotc
package interactive

import scala.io.Codec

import ast.tpd
import core._, core.Decorators.{sourcePos => _}
import Contexts._, NameOps._, Symbols._, StdNames._
import util._, util.Positions._

/** A typechecked named `tree` coming from `source` */
case class SourceTree(tree: tpd.NameTree, source: SourceFile) {
  /** The position of `tree` */
  def pos(implicit ctx: Context): SourcePosition = source.atPos(tree.pos)

  /** The position of the name in `tree` */
  def namePos(implicit ctx: Context): SourcePosition = {
    // FIXME: Merge with NameTree#namePos ?
    val treePos = tree.pos
    if (treePos.isZeroExtent || tree.name.toTermName == nme.ERROR)
      NoSourcePosition
    else {
      // Constructors are named `<init>` in the trees, but `this` in the source.
      val nameLength = tree.name match {
        case nme.CONSTRUCTOR => nme.this_.toString.length
        case other => other.stripModuleClassSuffix.show.toString.length
      }
      val position = {
        // FIXME: This is incorrect in some cases, like with backquoted identifiers,
        //        see https://github.com/lampepfl/dotty/pull/1634#issuecomment-257079436
        val (start, end) =
          if (!treePos.isSynthetic)
            (treePos.point, treePos.point + nameLength)
          else
            // If we don't have a point, we need to find it
            (treePos.end - nameLength, treePos.end)
        Position(start, end, start)
      }
      source.atPos(position)
    }
  }
}

object SourceTree {
  def fromSymbol(sym: ClassSymbol, id: String = "")(implicit ctx: Context): Option[SourceTree] = {
    if (sym == defn.SourceFileAnnot || // FIXME: No SourceFile annotation on SourceFile itself
        sym.sourceFile == null) // FIXME: We cannot deal with external projects yet
      None
    else {
      import ast.Trees._
      def sourceTreeOfClass(tree: tpd.Tree): Option[SourceTree] = tree match {
        case PackageDef(_, stats) =>
          stats.flatMap(sourceTreeOfClass).headOption
        case tree: tpd.TypeDef if tree.symbol == sym =>
          val sourceFile = new SourceFile(sym.sourceFile, Codec.UTF8)
          Some(SourceTree(tree, sourceFile))
        case _ => None
      }
      sourceTreeOfClass(sym.rootTreeContaining(id))
    }
  }
}
