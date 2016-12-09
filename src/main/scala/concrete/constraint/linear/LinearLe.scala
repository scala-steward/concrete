package concrete.constraint.linear

import com.typesafe.scalalogging.LazyLogging

import concrete.Contradiction
import concrete.Outcome
import concrete.ParameterManager
import concrete.ProblemState
import concrete.Variable
import concrete.constraint.Removals
import cspom.Statistic
import cspom.util.BitVector
import concrete.Domain
import concrete.util.Interval

object LinearLe {
  def apply(constant: Int, factors: Array[Int], scope: Array[Variable], strict: Boolean, pm: ParameterManager) = {
    val actualConstant = if (strict) constant - 1 else constant

    val (sf, ss, si) = Linear.sortIntervals(factors, scope)

    new LinearLe(actualConstant, sf, ss, si)
  }

  @Statistic
  var shaves = 0l

}

final class LinearLe(
  constant: Int,
  factors: Array[Int],
  scope: Array[Variable],
  val is: Array[Int]) extends Linear(constant, factors, scope, SumLE)
    with IncrementalBoundPropagation with Removals with LazyLogging {

  import IncrementalBoundPropagation._

  override def consistent(ps: ProblemState, mod: BitVector) = {
    val (doms, f, vars, max) = updateF(ps, mod)
    clearMod()
    if (f.lb <= 0) {
      ps.updateState(this, (doms, f, vars, max))
    } else {
      Contradiction(scope)
    }
  }

  override def revise(ps: ProblemState, mod: BitVector): Outcome = {
    val (doms, f, vars, max) = updateF(ps, mod)
    //if (bc) {
      proceed(ps, doms, f, vars, max)
//    } else {
//      ps.updateState(this, (doms, f, vars, max))
//    }

  }

  def proceed(ps: ProblemState, doms: Array[Domain], f: Interval, vars: BitVector, max: Int) = {
    if (f.ub <= 0) {
      ps.entail(this)
    } else if (max <= -f.lb) {
      ps.updateState(this, (doms, f, vars, max))
    } else {
      processUB(vars.nextSetBit(0), vars, doms, f, factors, ps) match {
        case PContradiction => Contradiction(scope)
        case PFiltered(changed, entailed, newF, newVars, newMax) =>
          val out = filter(changed, doms, ps)
          if (entailed) {
            out.entail(this)
          } else {
            out.updateState(this, (doms, newF, newVars, newMax))
          }
      }

    }
  }

  override def toString() = toString("<=BC")

  override def toString(ps: ProblemState) = toString(ps, "<=BC")

  def getEvaluation(ps: ProblemState) = arity * 2

  def simpleEvaluation: Int = 3

  override def init(ps: ProblemState) = initData(ps)

}
