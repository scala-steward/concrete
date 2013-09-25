package concrete.generator.cspompatterns

import cspom.compiler.ConstraintCompilerNoData
import cspom.CSPOMConstraint
import cspom.CSPOM
import cspom.compiler.Delta

/**
 * If the given constraint is an all-different or neq constraint, remove it
 * if it is subsumed by another difference constraint.
 *
 * @param constraint
 */
object SubsumedDiff extends ConstraintCompilerNoData {

  def matchBool(constraint: CSPOMConstraint, problem: CSPOM) =
    AllDiff.ALLDIFF_CONSTRAINT(constraint) && haveSubsumingConstraint(constraint, problem, AllDiff.DIFF_CONSTRAINT)

  def compile(constraint: CSPOMConstraint, problem: CSPOM) = {
    problem.removeConstraint(constraint)
    new Delta(constraint)
  }

  private def haveSubsumingConstraint(
    constraint: CSPOMConstraint, problem: CSPOM, validator: CSPOMConstraint => Boolean) = {
    val smallestDegree = constraint.scope.minBy(problem.constraints(_).size)

    problem.constraints(smallestDegree).exists(
      c => c != constraint && validator(c) && constraint.scope.forall(c.scope.contains))
  }
}