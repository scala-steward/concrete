package concrete.runner

import concrete.ParameterManager
import cspom.StatisticsManager

class FZWriter(val params: ParameterManager, val problem: String, val stats: StatisticsManager) extends ConcreteWriter {

  if (params.contains("s"))
    Console.println(s"% $problem")

  for ((k, v) <- params.parameters) {
    Console.println(s"% $k = $v")
  }

  Console.println(s"% Concrete v$version running")




  def printSolution(sol: String, obj: Seq[(String, Any)]): Unit = {
    Console.println(sol)
    for ((ov, o) <- obj) {
      Console.println(s"% objective $ov = $o")
    }
    if (params.contains("a")) writeStats()
  }

  private def writeStats(): Unit = {
    if (params.contains("s"))
      for ((n, v) <- stats.digest.toSeq.sortBy(_._1)) {
        Console.println(s"% $n = $v")
      }
  }

  def error(e: Throwable): Unit = {
    e.printStackTrace(Console.err)
  }

  def disconnect(status: RunnerResult): Unit = {
    writeStats()
    Console.println {
      status match {
        case FullExplore if lastSolution.isDefined => "=========="
        case FullExplore => "=====UNSATISFIABLE====="
        case Unfinished(_) if lastSolution.isDefined => ""
        case _ => "=====UNKNOWN====="
      }
    }
  }

}
