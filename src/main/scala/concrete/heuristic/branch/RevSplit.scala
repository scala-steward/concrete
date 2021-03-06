package concrete.heuristic.branch

import concrete._
import concrete.heuristic.Decision

final class RevSplit() extends BranchHeuristic {

  override def toString = "rev-split"

  def compute(s: MAC, ps: ProblemState): ProblemState = ps

  override def branch(variable: Variable, domain: Domain, ps: ProblemState): Either[Outcome, (ProblemState, Decision, Decision)] = {
    Right(Split.revSplitAt(variable, domain, domain.median, ps))
  }

  def shouldRestart = false
}
