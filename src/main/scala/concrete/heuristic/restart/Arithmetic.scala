package concrete.heuristic.restart

import concrete.ParameterManager
import concrete.Problem

class Arithmetic(params: ParameterManager, problem: Problem) extends RestartStrategy {
  val growth: Int = params.getOrElse("arithmetic.growth", 100)

  val base: Int = params.getOrElse("arithmetic.base", 100)

  var maxBacktracks: Int = base

  def reset(): Unit = maxBacktracks = base

  def nextRun(): Option[Int] = {
    val bt = maxBacktracks
    maxBacktracks += growth
    Some(bt)
  }

}