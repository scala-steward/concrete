package concrete.heuristic;

import scala.collection.JavaConversions
import concrete.constraint.Constraint
import concrete.Problem
import concrete.Variable;
import scala.annotation.tailrec

final class DDegOnDom(val problem: Problem) extends VariableHeuristic with RandomBreak {

  def score(variable: Variable) =
    variable.getDDegEntailed.toDouble / variable.dom.size

  override def toString = "max-ddeg/dom"

}