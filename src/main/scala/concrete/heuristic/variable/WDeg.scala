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
package heuristic
package variable

import java.util.EventObject

final class WDeg(val pool: Seq[Variable]) extends ScoredVariableHeuristic
  with ConstraintWeighting {

  def score(variable: Variable, dom: Domain, state: ProblemState): Double =
    state.wDeg(variable) //.getWDegEntailed(state)

  override def toString = s"max-wdeg"

  override def shouldRestart = true

}

trait ConstraintWeighting extends ScoredVariableHeuristic {

  override def compute(s: MAC, ps: ProblemState): ProblemState = {
    for (v <- pool) {
      for (c <- v.constraints) {
        c.weight = 1
        v.weight += 1
      }
    }
    ps // super.compute(s, ps)
  }


  override def event[S <: Outcome](e: EventObject, ps: S): S = {
    if (e == ContradictionEvent) {
      val c = ps.asInstanceOf[Contradiction]
      c.cause.get.incrementWeight()
    }
    ps
  }
}