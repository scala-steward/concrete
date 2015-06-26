package concrete.constraint.semantic;

import concrete.Contradiction
import concrete.ProblemState
import concrete.Variable
import concrete.constraint.BC
import concrete.constraint.Constraint
import concrete.util.Interval

final class AbsDiffBC(val result: Variable, val v0: Variable, val v1: Variable)
  extends Constraint(Array(result, v0, v1)) with BC {

  def check(t: Array[Int]) = t(0) == math.abs(t(1) - t(2))

  private def union(i0: Option[Interval], i1: Option[Interval]): Option[Interval] = {
    i0.map {
      i: Interval =>
        if (i1.isDefined) {
          i span i1.get
        } else {
          i
        }
    }
      .orElse(i1)
  }

  def shave(ps: ProblemState) = {
    val rspan = ps.span(result)
    val v1span = ps.span(v1)
    val v0span = ps.span(v0)

    ps.shaveDom(result, (v0span - v1span).abs)
      .andThen { ps =>

        val rspan = ps.dom(result).span

        union(v0span.intersect(v1span - rspan), v0span.intersect(v1span + rspan)) match {
          case None => Contradiction
          case Some(v0span) =>
            ps.shaveDom(v0, v0span).andThen { ps =>
              union(
                v1span.intersect(v0span - rspan),
                v1span.intersect(v0span + rspan)) match {
                  case None    => Contradiction
                  case Some(g) => ps.shaveDom(v1, g)
                }
            }
        }
      }

  }

  override def toString(ps: ProblemState) =
    s"${result.toString(ps)} =BC= |${v0.toString(ps)} - ${v1.toString(ps)}|";

  def advise(ps: ProblemState, pos: Int) = 5

  def simpleEvaluation = 2
}