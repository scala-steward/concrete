package concrete
package constraint
package semantic

import concrete.util.Interval
import scala.collection.mutable.ArrayBuffer
import concrete.util.Math
import java.util.Arrays
import java.util.Collections

object BinPacking {
  def apply(load: Seq[Variable], offset: Int, bin: Seq[Variable], weight: Seq[Int]) = {
    val (sBin, sW) = (bin zip weight).sortBy(-_._2).unzip
    new BinPacking(load.toArray, offset, sBin.toArray, sW.toArray)
  }
}

class BinPacking private (load: Array[Variable], offset: Int, assignments: Array[Variable], weight: Array[Int]) extends Constraint(load ++ assignments)
    with BC {
  def advise(ps: concrete.ProblemState, position: Int): Int = load.length + assignments.length
  def check(tuple: Array[Int]): Boolean = ???

  def init(ps: concrete.ProblemState): Outcome = {
    //    val c = ps.doms(load).map(_.last).max
    //    println(l2(weight, c))
    ps.fold(0 until assignments.length) { (ps, i) =>
      ps.shaveDom(assignments(i), offset, load.length + offset - 1)
    }
  }

  private val totalWeights = weight.sum

  private def elimAndCommit(ps: ProblemState, i: Int, sumCandidates: Array[Int], assigned: Array[Int], bounds: Array[Interval]): Outcome = {
    val dom = ps.dom(assignments(i))

    if (dom.isAssigned) {
      ps
    } else {

      // 4. Commitment
      val commitment = dom.find { j => sumCandidates(j - offset) - weight(i) < bounds(j - offset).lb }
        .map(dom.assign)
        .getOrElse(dom)
        // 3. Single item elimination
        .filter { j => assigned(j - offset) + weight(i) <= bounds(j - offset).ub }

      if (commitment.isEmpty) Contradiction(scope)
      else if (commitment eq dom) {
        ps
      } else {

        for (k <- dom) { sumCandidates(k - offset) -= weight(i) }
        if (commitment.isAssigned) {
          val j = commitment.singleValue
          assigned(j - offset) += weight(i)
          sumCandidates(j - offset) += weight(i)
        } else for (j <- commitment) {
          sumCandidates(j - offset) += weight(i)
        }

        ps.updateDomNonEmptyNoCheck(assignments(i), commitment)
      }
    }
  }

  private def computeState(ps: ProblemState): (Array[Int], Array[Int]) = {
    val assigned = new Array[Int](load.length)
    val sumCandidates = new Array[Int](load.length)

    for (b <- 0 until assignments.length) {

      val dom = ps.dom(assignments(b))
      //println(dom)
      if (dom.isAssigned) {
        val r = dom.singleValue - offset
        assigned(r) += weight(b)
        sumCandidates(r) += weight(b)
      } else {
        for (i <- dom) {
          sumCandidates(i - offset) += weight(b)
        }
      }
    }
    (assigned, sumCandidates)
  }

  private def compCandidates(ps: ProblemState): Array[ArrayBuffer[Int]] = {
    val candidates = Array.fill(load.length)(new ArrayBuffer[Int](assignments.length))

    for (
      b <- 0 until assignments.length
    ) {
      val dom = ps.dom(assignments(b))

      if (!dom.isAssigned) {
        for (i <- dom) {
          candidates(i - offset) += weight(b)
        }
      }
    }

    candidates
  }

  private def checkFeasibility(ps: ProblemState, bin: Int, candidates: ArrayBuffer[Int], assigned: Array[Int]): Outcome = {
    // Pruning rule
    val dom = ps.dom(load(bin))

    if (noSum(candidates, dom.head - assigned(bin), dom.last - assigned(bin)).isDefined) {
      // println(s"candidates: ${candidates.toSeq}, bounds: ${(dom.head - assigned(j), dom.last - assigned(j))}")
      Contradiction(scope(bin))
    } else {
      val d1 = noSum(candidates, dom.head - assigned(bin), dom.head - assigned(bin)) match {
        case Some((_, b)) => dom.removeUntil(assigned(bin) + b)
        case _ => dom
      }

      val d2 = noSum(candidates, dom.last - assigned(bin), dom.last - assigned(bin)) match {
        case Some((a, _)) => d1.removeAfter(assigned(bin) + a)
        case _ => d1
      }

      ps.updateDom(load(bin), d2)

    }
  }

  override def shave(ps: ProblemState): Outcome = {
    val (assigned, sumCandidates) = computeState(ps)

    // 1. Load Maintenance
    val bounds = new Array[Interval](load.length)

    var sumLB = 0
    var sumUB = 0

    for (j <- 0 until load.length) {
      bounds(j) = ps.span(load(j)).fastIntersect(assigned(j), sumCandidates(j))
      sumLB += bounds(j).lb
      sumUB += bounds(j).ub
    }

    // 2. Load and size coherence
    for (j <- 0 until load.length) {
      val b = bounds(j)
      sumLB -= b.lb
      sumUB -= b.ub

      bounds(j) = b.fastIntersect(totalWeights - sumUB, totalWeights - sumLB)
      sumLB += bounds(j).lb
      sumUB += bounds(j).ub
    }

    ps.fold(0 until load.length) { (ps, i) => ps.shaveDom(load(i), bounds(i)) }
      .fold(0 until assignments.length)((ps, i) => elimAndCommit(ps, i, sumCandidates, assigned, bounds))
      .andThen { ps =>
        val candidates = compCandidates(ps)
        ps.fold(0 until load.length)((ps, j) => checkFeasibility(ps, j, candidates(j), assigned))
      }
      .andThen { ps =>
        // Use a lower bound on the number of required bins with partial solutions

        // Compute maximum load
        var C = 0
        for (j <- 0 until load.length) {
          C = math.max(C, ps.dom(load(j)).last)
        }

        // Fake items to take into account assigned items and reduced capacity wrt C
        val a = new Array[Int](load.length)

        for (j <- 0 until load.length) {
          a(j) = assigned(j) + C - ps.dom(load(j)).last
        }

        Arrays.sort(a)
        reverse(a)

        // List of unassigned items
        val u = Iterator.range(0, assignments.length).filter(i => !ps.assigned(assignments(i))).map(weight).toArray

        if (l2(merge(a, u), C) > load.length) Contradiction(scope) else ps
      }
  }

  private def reverse(a: Array[Int]): Unit = {
    for (i <- 0 until a.length / 2) {
      val t = a(i)
      a(i) = a(a.length - i - 1)
      a(a.length - i - 1) = t
    }
  }

  private def merge(a: Array[Int], b: Array[Int]): Array[Int] = {
    val dest = new Array[Int](a.length + b.length)
    var i, j, k = 0
    while (i < a.length && j < b.length) {
      if (a(i) > b(j)) {
        dest(k) = a(i)
        i += 1
      } else {
        dest(k) = b(j)
        j += 1
      }
      k += 1
    }

    System.arraycopy(b, j, dest, k, b.length - j)
    System.arraycopy(a, i, dest, k, a.length - i)

    dest

  }

  private def noSum(X: IndexedSeq[Int], α: Int, β: Int): Option[(Int, Int)] = {
    if (α <= 0 || β >= X.sum) None
    else {
      val N = X.length - 1
      var ΣA, ΣB, ΣC = 0 // See figure 1 for meaning of A, B, C
      var k = 0
      var kp = 0 // k largest items, kp smallest items
      while (ΣC + X(N - kp) < α) {
        ΣC += X(N - kp)
        kp += 1
      }
      ΣB = X(N - kp)
      while (ΣA < α && ΣB <= β) {
        ΣA += X(k)
        k += 1 // change from original paper
        if (ΣA < α) {
          kp -= 1
          ΣB += X(N - kp)
          ΣC -= X(N - kp)
          while (ΣA + ΣC >= α) {
            kp -= 1
            ΣC -= X(N - kp)
            ΣB += X(N - kp) - X(N - kp - k - 1)
          }
        }

      }
      if (ΣA < α) Some((ΣA + ΣC, ΣB)) else None
    }
  }

  /**
   * Computes a lower bound on the number of required bins of capacity C to store items X (sorted non-incrementally)
   *
   * S. Martello and P. Toth.
   * Lower bounds and reduction procedures for the bin packing problem.
   * Discrete and Applied Mathematics, 28(1):59–70, 1990.
   */
  private def l2(X: Array[Int], C: Int): Int = {

    if (X.last > C / 2) X.length
    else {
      var i = 0
      while (X(i) > C / 2) i += 1

      val sumSv = X.view.drop(i).sum

      var best = Math.ceilDiv(X.sum, C)

      do {
        val K = X(i)
        var n1, n2, n3, n2free = 0
        for (x <- X) {
          if (x > C - K) n1 += 1
          else if (x > C / 2) {
            n2 += 1
            n2free += C - x
          } else if (x >= K) n3 += x
        }
        val current = n1 + n2 + math.max(0, Math.ceilDiv(n3 - n2free, C))

        best = math.max(best, current)

        val end = n1 + n2 + Math.ceilDiv(sumSv - n2free, C)

        if (end <= best) return best
        i += 1
      } while (true)
      best
    }
  }
  def simpleEvaluation: Int = ???
}