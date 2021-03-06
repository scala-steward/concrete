/**
  * CSPFJ - CSP solving API for Java
  * Copyright (C) 2006 Julien VION
  *
  * This library is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 2.1 of the License, or (at your option) any later version.
  *
  * This library is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this library; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
  */

package concrete
package constraint
package semantic

import bitvectors.BitVector

trait AllDiffChecker extends Constraint {

  private val offset = scope.iterator.map(_.initDomain.head).min

  def except: Set[Int]

  def check(t: Array[Int]): Boolean = {
    //println(t.toSeq)
    val union = collection.mutable.BitSet.empty

    !t.exists { v =>
      !except.contains(v) && union(v - offset) || {
        union += (v - offset)
        false
      }
    }
  }
}

final class AllDifferent2C(scope: Array[Variable], val except: Set[Int] = Set()) extends Constraint(scope) with AllDiffChecker with AdviseCounts {

  val simpleEvaluation = 3
  var q: List[Int] = Nil
  private var lastAdvise = -1

  def init(ps: ProblemState): ProblemState = ps

  def revise(ps: ProblemState, mod: BitVector): Outcome = {
    // val queue = scope.indices.filter(i=>ps.dom(scope(i)).isAssigned).toList
    val out = revise(ps, q).entailIfFree(this)
    q = Nil
    out
  }

  def revise(ps: ProblemState, queue: List[Int]): Outcome = queue match {
    case Nil => ps
    case head :: tail =>
      var state = ps
      var q = tail
      val value = ps.dom(scope(head)).singleValue
      if (!except(value)) {
        var i = arity - 1
        while (i >= 0) {
          if (i != head) {
            val v = scope(i)
            val od = state.dom(v)
            if (od.contains(value)) {
              val nd = od - value

              if (od ne nd) {
                if (nd.isEmpty) return Contradiction(v)
                state = state.updateDomNonEmptyNoCheck(v, nd)
                if (nd.isAssigned) q ::= i
              }
            }
          }
          i -= 1
        }
      }

      revise(state, q)
  }

  def advise(ps: ProblemState, event: Event, p: Int): Int = {
    if (lastAdvise != adviseCount) {
      q = Nil
      lastAdvise = adviseCount
    }
    // event is set to Assignment during initialization,
    // so check for actual assignment
    if (event <= Assignment) {
      val dom = ps.dom(scope(p))
      if (dom.isAssigned && !except(dom.head)) {
        q ::= p
        arity
      } else {
        -1
      }
    } else {
      -1
    }
  }
}
