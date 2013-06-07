package concrete.constraint.semantic

import concrete.Variable
import concrete.constraint.Constraint
import concrete.constraint.Residues
import concrete.constraint.TupleEnumerator

class Occurrence(val result: Variable, val value: Int, val vars: Array[Variable])
  extends Constraint(result +: vars) {

  def checkValues(tuple: Array[Int]) =
    tuple(0) == (1 until arity).count(i => tuple(i) == value)

  /**
   *  As seen from class Occurrence, the missing signatures are as follows.
   *   For convenience, these are usable as stub implementations.
   */

  def advise(pos: Int): Int = arity

  def revise(): Traversable[Int] = {
    var affected = 0
    var canBeAffected = 0

    for (v <- vars) {
      if (v.dom.presentVal(value)) {
        if (v.dom.size == 1) {
          affected += 1
        } else {
          canBeAffected += 1
        }
      }
    }

    var ch = if (result.dom.intersectVal(affected, affected + canBeAffected)) {
      List(0)
    } else {
      Nil
    }

    if (affected == result.dom.lastValue && canBeAffected > 0) {
      ch ::: (1 until arity).filter(scope(_).dom.removeVal(value)).toList
    } else if (result.dom.firstValue == affected + canBeAffected) {
      ch ::: (1 until arity).filter(
        p =>
          if (scope(p).dom.present(value)) {
            scope(p).dom.setSingle(value)
            true
          } else {
            false
          }).toList
    } else {
      ch
    }

  }

  def simpleEvaluation: Int = ???

}