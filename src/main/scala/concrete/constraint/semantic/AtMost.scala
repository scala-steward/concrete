package concrete.constraint.semantic

import bitvectors.BitVector
import com.typesafe.scalalogging.LazyLogging
import concrete.constraint.{Constraint, StatefulConstraint}
import concrete._

class AtMost(val result: Variable, val value: Variable,
             val vars: Array[Variable])
  extends Constraint(result +: value +: vars) with StatefulConstraint[Map[Int, BitVector]] with LazyLogging {

  override def init(ps: ProblemState): ProblemState = {
    val initMap = ps.dom(value).view.map(i => i -> BitVector.empty).toMap

    ps.updateState(this, recomputeState(ps, initMap, BitVector.filled(arity), ps.dom(value)))
  }

  private def recomputeState(ps: ProblemState, affect: Map[Int, BitVector], mod: BitVector, currentValues: Domain): Map[Int, BitVector] = {
    var affected = affect
    var sm = mod.nextSetBit(2)
    while (sm >= 0) {
      val m = sm - 2
      val dom = ps.dom(vars(m))
      if (dom.isAssigned) {
        val value = dom.head
        /* Only process values that appear in the "value" variable */
        if (currentValues.contains(value)) {
          affected = affected.updated(value, affected(value) + m)
        }
      }

      sm = mod.nextSetBit(sm + 1)
    }

    assert(
      currentValues.forall { v =>
        affected(v).toSeq == vars.indices.filter { p =>
          val d = ps.dom(vars(p))
          d.isAssigned && d.head == v
        }
      },

      s"incorrect affected for ${toString(ps)}: $affected after $mod")

    affected
  }

  override def toString(ps: ProblemState) = s"At most ${result.toString(ps)} occurrences of ${value.toString(ps)} in (${vars.map(_.toString(ps)).mkString(", ")})"

  def check(tuple: Array[Int]): Boolean = {
    tuple(0) >= (2 until arity).count(i => tuple(i) == tuple(1))
  }

  def advise(ps: ProblemState, event: Event, pos: Int): Int = arity

  override def toString = s"At most $result occurrences of $value in (${vars.mkString(", ")})"

  def revise(ps: ProblemState, mod: BitVector): Outcome = {
    val currentValues = ps.dom(value)

    val affected = recomputeState(ps, ps(this), mod, currentValues)

    filterResult(ps, currentValues, affected)
      .andThen(filterValue(_, affected))
      .andThen(filterVars(_, affected))
      .updateState(this, affected)
  }

  private def filterResult(ps: ProblemState, currentValues: Domain, affected: Map[Int, BitVector]) = {
    var min = Int.MaxValue
    currentValues.foreach(value => min = math.min(min, affected(value).cardinality))
    ps.removeUntil(result, min)
  }

  private def filterValue(ps: ProblemState, affected: Map[Int, BitVector]) = {
    val bound = ps.dom(result).last
    ps.filterDom(value) { v =>
      affected(v).cardinality <= bound
    }
  }

  private def filterVars(ps: ProblemState, affected: Map[Int, BitVector]) = {
    val values = ps.dom(value)

    if (values.isAssigned) {
      val value = values.head
      val bound = ps.dom(result).last

      if (affected(value).cardinality == bound) {
        // Maximum nb of values is affected, removing from other variables
        vars
          .foldLeft(ps) { (ps, v) =>
            val d = ps.dom(v)
            if (!d.isAssigned && d.contains(value)) {
              ps.updateDomNonEmpty(v, d - value)
            } else {
              ps
            }
          }
          .entail(this)
      } else {
        ps
      }
    } else {
      ps
    }
  }

  def simpleEvaluation: Int = 3

}