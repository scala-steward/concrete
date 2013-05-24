package concrete.heuristic;

import concrete.Problem
import concrete.Variable
import concrete.util.TieManager
import scala.util.Random
import scala.annotation.tailrec

trait RandomBreak extends VariableHeuristic {

  private val rand = new Random(0)

  @tailrec
  private def select(list: Iterator[Variable], best: Variable, ties: Int): Variable = {
    if (list.isEmpty) { best }
    else {
      val current = list.next
      val comp = compare(current, best)

      if (comp > 0) {
        select(list, current, 2)
      } else if (comp == 0) {
        if (rand.nextDouble() * ties < 1) {
          select(list, current, ties + 1)
        } else {
          select(list, best, ties + 1)
        }

      } else {
        select(list, best, ties)
      }
    }
  }

  override def select(itr: Iterator[Variable]): Option[Variable] = {
    if (itr.isEmpty) { None }
    else { Some(select(itr, itr.next, 2)) }
  }

}