package concrete.constraint.semantic;

import scala.annotation.tailrec
import concrete.BooleanDomain
import concrete.Domain
import concrete.FALSE
import concrete.TRUE
import concrete.Variable
import concrete.constraint.Constraint
import concrete.Contradiction
import concrete.ProblemState
import concrete.Outcome

final class Clause(positive: Array[Variable], negative: Array[Variable]) extends Constraint(positive ++ negative) {

  val posLength = positive.length

  //require(reverses.size == scope.size, "reverses must cover all variables")

  private var watch1: Int = (0 until posLength)
    .find(p => scope(p).initDomain.asInstanceOf[BooleanDomain].canBe(true))
    .orElse {
      (posLength until arity)
        .find(p => scope(p).initDomain.asInstanceOf[BooleanDomain].canBe(false))
    }
    .get

  private var watch2: Int = -1

  private val ids = scope.map(_.id)

  //if (isTrue(watch1) || isTrue(watch2)) entail()

  def advise(ps: ProblemState, p: Int) = if (p == watch1 || p == watch2) 1 else -1

  override def check(t: Array[Int]) = {
    val (p, n) = t.view.splitAt(positive.length)
    p.contains(1) || n.contains(0)
  }

  override def toString(ps: ProblemState) =
    "\\/" + (positive.map(_.toString(ps)) ++ negative.map(v => "-" + v.toString(ps))).mkString("(", ", ", ")")

  def reviseW2(ps: ProblemState): Outcome = {
    if (watch2 < 0 || isFalse(ps, watch2)) {
      val w = seekWatch(ps, watch1)
      if (w < 0) {
        enforce(ps, watch1).entail(this)
      } else {
        watch2 = w
        reviseW1(ps)
      }
    } else {
      reviseW1(ps)
    }
  }

  def reviseW1(ps: ProblemState): Outcome = {
    if (isTrue(ps, watch2)) {
      ps.entail(this)
    } else if (isFalse(ps, watch1)) {
      val w = seekWatch(ps, watch2)
      if (w < 0) {
        enforce(ps, watch2).entail(this)
      } else {
        watch1 = w
        if (isTrue(ps, w)) {
          ps.entail(this)
        } else {
          ps
        }
      }

    } else {
      ps
    }
  }

  def revise(ps: ProblemState): Outcome = {
    if (isTrue(ps, watch1)) { ps.entail(this) }
    else { reviseW2(ps) }
  }

  private def enforce(ps: ProblemState, position: Int): Outcome = {
    if (position < posLength) {
      ps.assign(ids(position), 1)
    } else {
      ps.assign(ids(position), 0)
    }
  }

  private def isTrue(ps: ProblemState, pos: Int): Boolean = {
    if (pos < posLength) {
      ps.dom(ids(pos)) == TRUE
    } else {
      ps.dom(ids(pos)) == FALSE
    }
  }

  private def isFalse(ps: ProblemState, pos: Int): Boolean = {
    if (pos < posLength) {
      ps.dom(ids(pos)) == FALSE
    } else {
      ps.dom(ids(pos)) == TRUE
    }
  }

  private def seekWatch(ps: ProblemState, excluding: Int): Int = {
    var i = 0
    while (i < posLength) {
      if (i != excluding && ps.boolDom(ids(i)).canBe(true)) {
        return i
      }
      i += 1
    }
    while (i < arity) {
      if (i != excluding && ps.boolDom(ids(i)).canBe(false)) {
        return i
      }
      i += 1
    }
    -1
  }

  val simpleEvaluation = 1
}
