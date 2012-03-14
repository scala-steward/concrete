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

package cspfj.constraint.extension;

import cspfj.constraint.AbstractConstraint
import cspfj.problem.Variable
import cspfj.util.BitVector
import scala.collection.JavaConversions
import scala.collection.mutable.BitSet
import cspfj.constraint.Removals
import cspfj.constraint.Constraint
import scala.annotation.tailrec
import cspfj.util.Loggable

final class ExtensionConstraintDynamic(
  scope: Array[Variable], matrix: TupleSet, shared: Boolean) extends AbstractConstraint(scope)
  with ExtensionConstraint with Removals with Loggable {

  val matrixManager = new MatrixManagerDynamic(scope, matrix, shared, tuple)

  private val found =
    (0 until arity) map (p => BitVector.newBitVector(scope(p).dom.maxSize)) toArray

  override def setLvl(l: Int) {
    super.setLvl(l)
    matrixManager.setLevel(l)
  }

  override def restoreLvl(l: Int) {
    super.restoreLvl(l)
    matrixManager.restoreLevel(l)
  }

  def revise(modified: Seq[Int]) = {
    //fine("Revising " + this + " : " + mod.toList)
    found.foreach(_.fill(false))

    val mod = modified.toList

    matrixManager.filterTuples { tuple =>
      if (controlTuplePresence(tuple, mod)) {
        assert(controlTuplePresence(tuple), tuple.mkString("(", ", ", ")") +
          " is not in " + scope.mkString("(", ", ", ")") + " with modified: " + modified)
        setFound(tuple, found, arity - 1)
        true
      } else false
    }

    val c = filter(found, arity - 1, false)

    val card = scope.map(v => BigInt(v.dom.size)).product
    assert(card >= matrixManager.size, card + " < " + matrixManager.size + "!")
    if (card == matrixManager.size) {
      //logger.info("Entailing " + this)
      entail()
    }

    c

  }

  @tailrec
  private def setFound(tuple: Array[Int], found: Array[BitVector], p: Int) {
    if (p >= 0) {
      found(p).set(tuple(p))
      setFound(tuple, found, p - 1)
    }
  }

  private def controlTuplePresence(tuple: Array[Int], mod: Seq[Int]) = {
    /** Need high optimization */

    @tailrec
    def control(m: Seq[Int]): Boolean =
      if (m.isEmpty) true
      else {
        val i = m.head
        scope(i).dom.present(tuple(i)) && control(m.tail)
      }

    control(mod)
  }

  private def filter(found: Array[BitVector], p: Int, c: Boolean): Boolean =
    if (p < 0) c
    else {
      val ch = scope(p).dom.filter(i => found(p)(i))
      filter(found, p - 1, c || ch)
    }

  def check = matrixManager.check

  def removeTuple(tuple: Array[Int]) = matrixManager.removeTuple(tuple)

  private def matches(tuple: Array[Int], base: Array[Int]) = {
    assert(tuple.length == base.length);
    (base, tuple).zipped.forall { (b, t) => b < 0 || b == t }
  }

  def removeTuples(base: Array[Int]) = {
    throw new UnsupportedOperationException
    matrixManager.unshareMatrix()
    val s = matrixManager.size

    //matrixManager.filter(t => !matches(t, base))

    matrixManager.size - s;
  }

  //def matrixManager = matrixManager

  def getEvaluation = arity * matrixManager.size

  def simpleEvaluation = math.min(7, scope.count(_.dom.size > 1))

  override def toString = "STR(" + scope.mkString(", ") + ") w/ " + matrixManager

}
