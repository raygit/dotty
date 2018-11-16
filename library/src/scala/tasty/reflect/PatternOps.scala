package scala.tasty
package reflect

trait PatternOps extends Core {

  trait PatternAPI {
    /** Position in the source code */
    def pos(implicit ctx: Context): Position

    def tpe(implicit ctx: Context): Type
  }
  implicit def PatternDeco(pattern: Pattern): PatternAPI

  val Pattern: PatternModule
  abstract class PatternModule {

    val Value: ValueExtractor
    abstract class ValueExtractor {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[Term]
    }

    val Bind: BindExtractor
    abstract class BindExtractor {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[(String, Pattern)]
    }

    val Unapply: UnapplyExtractor
    abstract class UnapplyExtractor {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[(Term, List[Term], List[Pattern])]
    }

    val Alternative: AlternativeExtractor
    abstract class AlternativeExtractor {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[List[Pattern]]
    }

    val TypeTest: TypeTestExtractor
    abstract class TypeTestExtractor {
      def unapply(pattern: Pattern)(implicit ctx: Context): Option[TypeTree]
    }
  }

}
