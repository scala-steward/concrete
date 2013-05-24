package concrete.heuristic;

import java.util.Comparator
import concrete.Variable
import concrete.Problem
import scala.math.Ordering.DoubleOrdering

trait VariableHeuristic extends Ordering[Variable] {
  def problem: Problem

  def select: Option[Variable] =
    select(problem.variables.iterator.filter(_.dom.size > 1))

  def select(coll: Iterator[Variable]) = {
    //    for (v <- coll) {
    //      println(v + " : " + score(v))
    //    }

    if (coll.isEmpty) { None }
    else { Some(coll.maxBy(score)) }
  }

  def score(variable: Variable): Double

  def compare(v1: Variable, v2: Variable) = {
    //println(v1 + ", " + v2 + " : " + score(v1).compare(score(v2)))
    score(v1).compare(score(v2))
  }

}