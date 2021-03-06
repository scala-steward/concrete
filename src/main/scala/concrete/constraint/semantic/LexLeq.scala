package concrete.constraint.semantic

;

import bitvectors.BitVector
import concrete._
import concrete.constraint.{Constraint, StatefulConstraint}

final class LexLeq(x: Array[Variable], y: Array[Variable]) extends Constraint(x ++ y)
  with StatefulConstraint[(Int, Int)] {

  type State = (Int, Int)

  private val n: Int = x.length
  require(n == y.length)

  def check(t: Array[Int]): Boolean = check(t, 0)

  def groundEq(x: Variable, y: Variable, ps: ProblemState): Boolean = {
    val xdom = ps.dom(x)
    xdom.size == 1 && {
      val ydom = ps.dom(y)
      ydom.size == 1 && xdom.singleValue == ydom.singleValue
    }
  }

  override def toString(ps: ProblemState): String = {
    s"$id: ${x.map(_.toString(ps)).mkString("[", ", ", "]")} <= ${y.map(_.toString(ps)).mkString("[", ", ", "]")} / " +
      Option(ps(this))
        .map {
          case (alpha, beta) => s"alpha = $alpha, beta = $beta"
        }
        .getOrElse("uninitalized")

  }

  override def init(ps: ProblemState): Outcome = {

    var alpha = 0
    while (alpha < n && groundEq(x(alpha), y(alpha), ps)) {
      alpha += 1
    }

    if (alpha == n) {
      ps.updateState(this, (alpha, n + 1))
    } else {
      var i = alpha
      var beta = -1
      while (i != n && min(x(i), ps) <= max(y(i), ps)) {
        if (min(x(i), ps) == max(y(i), ps)) {
          if (beta == -1) beta = i
        } else {
          beta = -1
        }
        i += 1
      }
      if (i == n) {
        beta = n + 1
      } else if (beta == -1) {
        beta = i
      }

      if (alpha == beta) {
        Contradiction(scope)
      } else {
        reEstablishGAC(alpha, ps.updateState(this, (alpha, beta)))
      }
    }

  }

  def revise(ps: ProblemState, cmod: BitVector): Outcome = {
    val mod = BitVector(cmod.view.map { m =>
      if (m < n) m else m - n
    })

    def reviseN(ps: ProblemState, i: Int): Outcome = {
      if (i < 0) ps
      else {
        reEstablishGAC(i, ps)
          .andThen {
            ps => reviseN(ps, mod.nextSetBit(i + 1))
          }
      }
    }

    reviseN(ps, mod.nextSetBit(0))
    //    val out = r match {
    //      case Contradiction    => "Contradiction"
    //      case ns: ProblemState => if (ns eq ps) "NOP" else if (ns.domains eq ps.domains) ns(this) else toString(ns)
    //    }
    //    println(s"${toString(ps)}, $mod -> $out")

  }

  def advise(ps: ProblemState, event: Event, pos: Int): Int = n

  //    case Nil => ps
  //    case head :: tail =>
  //      reEstablishGAC(head, ps).andThen(ps => reviseN(ps, tail))
  //  }

  def simpleEvaluation = 2

  @annotation.tailrec
  private def check(t: Array[Int], i: Int): Boolean =
    i >= n ||
      t(i) < t(i + n) || (t(i) == t(i + n) && check(t, i + 1))

  private def min(v: Variable, ps: ProblemState) = ps.dom(v).head

  private def max(v: Variable, ps: ProblemState) = ps.dom(v).last

  /**
    * Triggered when min(x(i)) or max(y(i)) changes
    */
  private def reEstablishGAC(i: Int, ps: ProblemState): Outcome = {
    val (alpha, beta) = ps(this)

    if (i == alpha) {
      if (i + 1 == beta) {
        establishAC(x(i), y(i), strict = true, ps)
      } else if (i + 1 < beta) {
        establishAC(x(i), y(i), strict = false, ps).andThen {
          ps =>
            if (groundEq(x(i), y(i), ps)) {
              updateAlpha(alpha + 1, beta, ps)
            } else {
              ps
            }
        }
      } else {
        ps
      }
    } else if (alpha < i && i < beta) {
      val minxi = min(x(i), ps)
      val maxyi = max(y(i), ps)
      if ((i == beta - 1 && minxi == maxyi) || minxi > maxyi) {
        updateBeta(i - 1, alpha, ps)
      } else {
        ps
      }
    } else {
      ps
    }
  }

  private def establishAC(x: Variable, y: Variable, strict: Boolean, ps: ProblemState): Outcome = {
    if (strict) {
      ps.removeTo(y, min(x, ps))
        .removeFrom(x, max(y, ps))
    } else {
      ps.removeUntil(y, min(x, ps))
        .removeAfter(x, max(y, ps))
    }
  }

  private def updateAlpha(alpha: Int, beta: Int, ps: ProblemState): Outcome = {
    if (alpha == n) {
      ps.updateState(this, (alpha, beta))
    } else if (alpha == beta) {
      Contradiction(scope)
    } else if (groundEq(x(alpha), y(alpha), ps)) {
      updateAlpha(alpha + 1, beta, ps)
    } else {
      reEstablishGAC(alpha, ps.updateState(this, (alpha, beta)))
    }
  }

  private def updateBeta(i: Int, alpha: Int, ps: ProblemState): Outcome = {
    val beta = i + 1
    if (alpha == beta) {
      Contradiction(scope)
    } else if (min(x(i), ps) < max(y(i), ps)) {
      if (i == alpha) {
        establishAC(x(i), y(i), strict = true, ps.updateState(this, (alpha, beta)))
      } else {
        ps.updateState(this, (alpha, beta))
      }
    } else {
      updateBeta(i - 1, alpha, ps)
    }

  }

}
