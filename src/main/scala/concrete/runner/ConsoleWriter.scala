package concrete.runner

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import cspom.StatisticsManager
import concrete.ParameterManager

class ConsoleWriter(val opts: Map[Symbol, Any], val stats: StatisticsManager) extends ConcreteWriter {

  def parameters(params: ParameterManager) {
    for ((k, v) <- params.parameters) {
      Console.println(s"% $k = $v")
    }
  }

  def problem(problem: String) {
    if (opts.contains('stats))
      Console.println(s"% $problem")
  }

  def solution(sol: String) {
    if (opts.contains('all)) writeStats()
    Console.println(sol)
  }

  def error(e: Throwable) {
    e.printStackTrace(Console.err)
  }

  private def writeStats(): Unit = {
    if (opts.contains('stats))
      for ((n, v) <- stats.digest.toSeq.sortBy(_._1)) {
        Console.println(s"% $n = $v")
      }
  }

  def disconnect(status: Try[Result]) {
    writeStats()
    Console.println {
      status
        .map {
          case SatFinished => "=========="
          case Unsat => "=====UNSATISFIABLE====="
          case SatUnfinished => ""
        }
        .getOrElse("=====UNKNOWN=====")
    }
  }

}
