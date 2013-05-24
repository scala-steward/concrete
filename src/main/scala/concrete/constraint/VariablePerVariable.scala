package concrete.constraint;

import scala.annotation.tailrec

import concrete.Variable
import concrete.util.Loggable

/**
 * A constraint that can be revised one variable at a time
 */
trait VariablePerVariable extends Constraint with Removals with Loggable {

  /**
   * Try to filter values from variable getVariable(position).
   *
   * @param position
   * @return true iff any value has been removed
   */
  def reviseVariable(position: Int, modified: List[Int]): Boolean

  def revise(modified: List[Int]) = {
    @tailrec
    def reviseNS(i: Int = arity - 1, c: List[Int] = Nil): List[Int] =
      if (i < 0) {
        c
      } else if (this.reviseVariable(i, modified)) {
        reviseNS(i - 1, i :: c)
      } else {
        reviseNS(i - 1, c)
      }

    @tailrec
    def reviseS(skip: Int, i: Int = arity - 1, c: List[Int] = Nil): List[Int] =
      if (i < 0) {
        c
      } else if (i == skip) {
        reviseNS(i - 1, c)
      } else if (this.reviseVariable(i, modified)) {
        reviseS(skip, i - 1, i :: c)
      } else {
        reviseS(skip, i - 1, c)
      }

    val c =
      if (modified.isEmpty) {
        Nil
      } else if (modified.tail.isEmpty) {
        reviseS(modified.head)
      } else {
        reviseNS()
      }

    entailCheck()
    c

  }

  @tailrec
  final def entailCheck(i: Int = scope.length - 1, c: Boolean = false) {

    if (i < 0) {
      entail()
    } else if (scope(i).dom.size <= 1) {
      entailCheck(i - 1, c)
    } else if (!c) {
      entailCheck(i - 1, true)
    }

  }

}