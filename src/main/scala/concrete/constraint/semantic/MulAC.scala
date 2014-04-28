package concrete.constraint.semantic;

import concrete.constraint.Constraint
import concrete.constraint.Residues
import concrete.Domain
import concrete.Variable
import concrete.constraint.Shaver
import concrete.util.Interval
import concrete.constraint.BCCompanion

/**
 * Contrainte V0 = V1 * V2.
 *
 * @author vion
 *
 */
final class MulAC(val result: Variable, val v0: Variable, val v1: Variable, val skipIntervals: Boolean = false)
  extends Constraint(Array(result, v0, v1))
  with Residues
  with BCCompanion {

  def checkValues(t: Array[Int]) = t(0) == (t(1) * t(2));

  private def reviseB(v: Variable, opp: Boolean, bounds: Interval) = {
    val myBounds = v.dom.valueInterval

    if (opp) {
      v.dom.intersectVal(bounds.lb + myBounds.ub, bounds.ub + myBounds.lb)
    } else {
      v.dom.intersectVal(bounds.ub / myBounds.ub, bounds.lb / myBounds.lb)
    }

  }

  def findSupport(variablePosition: Int, index: Int) =
    variablePosition match {
      case 0 => findValidTuple0(index);

      case 1 => findValidTupleV0(index);
      case 2 => findValidTupleV1(index);
      case _ =>
        throw new IndexOutOfBoundsException()

    }

  private def findValidTuple0(index: Int) = {
    val val0 = result.dom.value(index)
    val dom1 = scope(1).dom
    val dom2 = scope(2).dom

    dom1.indices.map { i =>
      val val1 = dom1.value(i);
      if (val1 == 0 && val0 == 0) (i, dom2.index(0))
      else if (val1 == 0 || val0 % val1 != 0) (i, -1)
      else (i, dom2.index(val0 / val1))
    } find {
      case (i, j) => j >= 0 && dom2.present(j)
    } map {
      case (i, j) => Array(index, i, j)
    }

  }

  def findValidTupleV0(index: Int) = {
    val result = this.result.dom
    val value = scope(1).dom.value(index)
    val dom = scope(2).dom
    dom.indices.map { i =>
      (i, result.index(value * dom.value(i)))
    } find {
      case (i, resIndex) => resIndex >= 0 && result.present(resIndex)
    } map {
      case (i, resIndex) => Array(resIndex, index, i)
    }
  }

  def findValidTupleV1(index: Int) = {
    val result = this.result.dom
    val value = scope(2).dom.value(index)
    val dom = scope(1).dom
    dom.indices.map { i =>
      (i, result.index(value * dom.value(i)))
    } find {
      case (i, resIndex) => resIndex >= 0 && result.present(resIndex)
    } map {
      case (i, resIndex) => Array(resIndex, i, index)
    }
  }

  override def toString = result + " = " + v0 + " * " + v1

  def getEvaluation = {
    val d0 = result.dom.size
    val d1 = v0.dom.size
    val d2 = v1.dom.size
    d0 * d1 + d0 * d2 + d1 * d2;
  }

  val simpleEvaluation = 2
}