package cspfj.constraint.semantic;

import cspfj.constraint.Residues
import cspfj.problem.Domain
import cspfj.problem.Variable;
import cspfj.constraint.AbstractConstraint

final class AbsDiff(val result: Variable, val v0: Variable, val v1: Variable)
  extends AbstractConstraint(Array(result, v0, v1)) with Residues {

  def check = value(0) == math.abs(value(1) - value(2))

  def shave() = {
    val i0 = v0.dom.valueInterval
    val i1 = v1.dom.valueInterval

    val diff = i0 - i1

    var ch = result.dom.intersectVal(diff.abs)
    val r = result.dom.valueInterval

    if (diff.lb > 0) {
      ch |= v0.dom.intersectVal(i1 + r)
      ch |= v1.dom.intersectVal(i0 - r)
    } else if (diff.ub < 0) {
      ch |= v0.dom.intersectVal(i1 - r)
      ch |= v1.dom.intersectVal(i0 + r)
    } else {
      ch |= v0.dom.intersectVal((i1 + r) union (i1 - r))
      ch |= v1.dom.intersectVal((i0 - r) union (i0 + r))
    }

    ch
  }

  override def revise() = {

    var ch = shave()
    while (ch && shave()) {}

    assert(!isBound || boundConsistent, this + " is not BC")

    ch | (!isBound && super.revise())

  }

  override def findSupport(position: Int, index: Int) =
    position match {
      case 0 =>
        if (scope(1).dom.size < scope(2).dom.size)
          findValidTuple0(index, 1, 2);
        else
          findValidTuple0(index, 2, 1);
      case 1 => findValidTuple(index, 1, 2);
      case 2 => findValidTuple(index, 2, 1);
      case _ => throw new IndexOutOfBoundsException;
    }

  def findValidTuple0(index: Int, pos1: Int, pos2: Int): Boolean = {
    val val0 = scope(0).dom.value(index)
    if (val0 < 0) {
      return false;
    }
    val dom1 = scope(pos1).dom
    val dom2 = scope(pos2).dom

    for (i <- dom1.indices) {

      val val1 = dom1.value(i);

      val j1 = dom2.index(val1 - val0);
      if (j1 >= 0 && dom2.present(j1)) {
        tuple(0) = index;
        tuple(pos1) = i;
        tuple(pos2) = j1;
        return true;
      }

      val j2 = dom2.index(val1 + val0);
      if (j2 >= 0 && dom2.present(j2)) {
        tuple(0) = index;
        tuple(pos1) = i;
        tuple(pos2) = j2;
        return true;
      }

    }
    return false;
  }

  def findValidTuple(index: Int, pos1: Int,
    pos2: Int): Boolean = {
    val result = this.result.dom
    val value = scope(pos1).dom.value(index);
    val dom = scope(pos2).dom;
    for (i <- dom.indices) {
      val resIndex = result.index(math.abs(value - dom.value(i)));
      if (resIndex >= 0 && result.present(resIndex)) {
        tuple(0) = resIndex;
        tuple(pos1) = index;
        tuple(pos2) = i;
        return true;
      }
    }
    return false;
  }

  override def toString = result + " = |" + v0 + " - " + v1 + "|";

  def getEvaluation = {
    val d0 = result.dom.size
    val d1 = v0.dom.size
    val d2 = v1.dom.size
    d0 * d1 + d0 * d2 + d1 * d2;
  }

  def simpleEvaluation = 2
}
