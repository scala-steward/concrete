package concrete.constraint.semantic;

import concrete.constraint.Constraint
import concrete.Domain
import concrete.Variable

object InInterval {
  def values(variable: Variable, lb: Int, ub: Int) =
    new InInterval(variable, variable.dom.closestGeq(lb), variable.dom.closestLeq(ub))

  def indices(variable: Variable, lb: Int, ub: Int) =
    new InInterval(variable, lb, ub)
}

/**
 * Constraint: variable \in [lb, ub]
 * lb and ub are domain indices!
 */
final class InInterval(val variable: Variable, val lb: Int, val ub: Int)
  extends Constraint(Array(variable)) {

  val dom = variable.dom

  def advise(p: Int) = 1

  def revise() = {
    val ch = dom.intersect(lb, ub)
    entail()
    if (ch) {
      0 :: Nil
    } else {
      Nil
    }
  }

  override def isConsistent() = dom.indices(lb).takeWhile(_ <= ub).exists(dom.present)

  def checkValues(t: Array[Int]) = {
    val value = t(0);
    lb <= value && value <= ub;
  }

  override def toString = variable + " in [" + variable.dom.value(lb) + ", " + variable.dom.value(ub) + "]"
  val simpleEvaluation = 1
}