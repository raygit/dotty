package dotty.tools.dotc.consumetasty

import dotty.tools.dotc.core.Phases.Phase
import dotty.tools.dotc.fromtasty._

import scala.tasty.file.TastyConsumer

class TastyFromClass(consumer: TastyConsumer) extends TASTYCompiler {

  override protected def frontendPhases: List[List[Phase]] =
    List(new ReadTastyTreesFromClasses) :: // Load classes from tasty
    Nil

  override protected def picklerPhases: List[List[Phase]] = Nil
  override protected def transformPhases: List[List[Phase]] = Nil

  override protected def backendPhases: List[List[Phase]] =
    List(new TastyConsumerPhase(consumer)) ::  // Print all loaded classes
    Nil
}
