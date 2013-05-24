package concrete.constraint.semantic;

import concrete.constraint.Constraint
import concrete.Domain
import concrete.Variable;

object NotInInterval {
  def values(variable: Variable, lb: Int, ub: Int) =
    new NotInInterval(variable, variable.dom.closestGeq(lb), variable.dom.closestLeq(ub))

  def indices(variable: Variable, lb: Int, ub: Int) =
    new NotInInterval(variable, lb, ub)
}

/**
 * Constraint: variable \notin [lb, ub]
 * lb and ub are domain indices!
 */
final class NotInInterval(val variable: Variable, val lb: Int, val ub: Int)
  extends Constraint(Array(variable)) {

  def advise(p: Int) = 1

  def revise() = {

    val ch = variable.dom.filter(i => lb < i || i > ub)

    entail()

    if (ch) {
      0 :: Nil
    } else {
      Nil
    }

  }

  override def isConsistent() =
    variable.dom.first < lb || variable.dom.last > ub;

  def checkValues(t: Array[Int]) = {
    val value = t(0);
    value < lb || ub < value;
  }

  override def toString = variable + " notin [" + variable.dom.value(lb) + ", " + variable.dom.value(ub) + "]"
  val simpleEvaluation = 1
}