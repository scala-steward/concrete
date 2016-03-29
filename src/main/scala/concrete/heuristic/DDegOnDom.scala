package concrete.heuristic;

import concrete.Domain
import concrete.ParameterManager
import concrete.ProblemState
import concrete.Variable

class DDegOnDom(params: ParameterManager, decisionVariables: Array[Variable]) extends ScoredVariableHeuristic(params, decisionVariables) {

  def score(variable: Variable, dom: Domain, state: ProblemState) =
    variable.getDDegEntailed(state).toDouble / dom.size

  override def toString = "max-ddeg/dom"

}
