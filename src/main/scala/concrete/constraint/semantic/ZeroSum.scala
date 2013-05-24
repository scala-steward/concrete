package concrete.constraint.semantic;

import scala.annotation.tailrec
import concrete.constraint.Constraint
import concrete.Domain
import concrete.util.Interval
import concrete.Variable
import concrete.util.Loggable
import concrete.constraint.Shaver
import scala.collection.mutable.HashSet

final class ZeroSum(
  val factors: Array[Int],
  scope: Array[Variable]) extends Constraint(scope)
  with Loggable {

  def this(scope: Array[Variable]) = this(Array.fill(scope.length)(1), scope)

  //val domFact = scope map (_.dom) zip factors toList

  def checkValues(t: Array[Int]): Boolean =
    (0 until arity).map(i => t(i) * factors(i)).sum >= 0

  def advise(p: Int) = arity

  def shave(): List[Int] = {

    var i = arity - 1

    var bounds = scope(arity - 1).dom.valueInterval * factors(i)

    i -= 1

    while (i >= 0) {
      bounds += scope(i).dom.valueInterval * factors(i)
      i -= 1
    }

    //val bounds = domFact map { case (d, f) => d.valueInterval * f } reduce (_ + _)

    //reduce (_ + _)
    //    val bounds = Interval(0, 0)

    var ch: List[Int] = Nil
    i = arity - 1
    while (i >= 0) {
      val dom = scope(i).dom
      val f = factors(i)
      val myBounds = dom.valueInterval * f

      val boundsf = Interval(bounds.lb - myBounds.lb, bounds.ub - myBounds.ub) / -f

      if (dom.intersectVal(boundsf)) {
        ch ::= i
      }
      i -= 1
    }
    ch
  }

  def revise() = {
    var mod = new HashSet[Int]()
    var ch = true
    while (ch) {
      ch = false
      val m = shave()
      if (m.nonEmpty) {
        ch = true
        mod ++= m
      }
    }
    mod
  }

  def reviseVariable(p: Int, mod: Seq[Int]) = false

  override def toString = (scope, factors).zipped.map((v, f) => f + "." + v).mkString(" + ") + " = 0"
  val simpleEvaluation = 3
}