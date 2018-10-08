package dotty.tools
package dotc
package util

import Positions.{Position, NoPosition}

import scala.annotation.internal.sharable

/** A source position is comprised of a position in a source file */
case class SourcePosition(source: SourceFile, pos: Position, outer: SourcePosition = NoSourcePosition)
extends interfaces.SourcePosition {
  /** Is `that` a source position contained in this source position ?
   *  `outer` is not taken into account. */
  def contains(that: SourcePosition): Boolean =
    this.source == that.source && this.pos.contains(that.pos)

  def exists: Boolean = pos.exists

  def lineContent: String = source.lineContent(point)

  def point: Int = pos.point

  /** The line of the position, starting at 0 */
  def line: Int = source.offsetToLine(point)

  /** Extracts the lines from the underlying source file as `Array[Char]`*/
  def linesSlice: Array[Char] =
    source.content.slice(source.startOfLine(start), source.nextLine(end))

  /** The lines of the position */
  def lines: List[Int] = {
    val startOffset = source.offsetToLine(start)
    val endOffset = source.offsetToLine(end + 1)
    if (startOffset >= endOffset) line :: Nil
    else (startOffset until endOffset).toList
  }

  def lineOffsets: List[Int] =
    lines.map(source.lineToOffset(_))

  def beforeAndAfterPoint: (List[Int], List[Int]) =
    lineOffsets.partition(_ <= point)

  /** The column of the position, starting at 0 */
  def column: Int = source.column(point)

  def start: Int = pos.start
  def startLine: Int = source.offsetToLine(start)
  def startColumn: Int = source.column(start)
  def startColumnPadding: String = source.startColumnPadding(start)

  def end: Int = pos.end
  def endLine: Int = source.offsetToLine(end)
  def endColumn: Int = source.column(end)

  def withOuter(outer: SourcePosition): SourcePosition = new SourcePosition(source, pos, outer)

  override def toString: String =
    if (source.exists) s"${source.file}:${line + 1}"
    else s"(no source file, offset = ${pos.point})"
}

/** A sentinel for a non-existing source position */
@sharable object NoSourcePosition extends SourcePosition(NoSource, NoPosition) {
  override def toString: String = "?"
  override def withOuter(outer: SourcePosition): SourcePosition = outer
}

