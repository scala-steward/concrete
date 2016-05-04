package concrete.runner.sql

import scala.xml.Node
import scala.xml.Text
import scala.xml.NodeSeq
import scala.collection.mutable.ListBuffer
import cspom.StatisticsManager
import scala.collection.mutable.HashMap
import scala.annotation.tailrec
import scala.collection.SortedMap
import slick.jdbc.GetResult
import slick.driver.PostgresDriver.api._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import java.io.File
import scala.util.Try
import scala.util.Failure

sealed trait ErrorHandling {
  def toDouble(stat: Double): Double
}
case object ErrorInfinity extends ErrorHandling {
  def toDouble(stat: Double) = Double.PositiveInfinity
}
case object ErrorKeep extends ErrorHandling {
  def toDouble(stat: Double) = stat
}
case object ErrorZero extends ErrorHandling {
  def toDouble(stat: Double) = 0.0
}
case object ErrorNaN extends ErrorHandling {
  def toDouble(stat: Double) = Double.NaN
}

object Table extends App {

  val format = "csv"

  def attributeEquals(name: String, value: String)(node: Node) = {
    node.attribute(name).get.exists(_ == Text(value))
  }

  def tabular(data: Seq[Any]) = {
    format match {
      case "latex" =>
        data.map {
          case d: String => d
          case d: Double => "\\np{%.1f}".format(d)
          case d: Int    => "\\np{%d}".format(d)
          case _         => "---"
        }.mkString(" & ") + "\\\\"

      case "csv" => data.mkString(", ")

    }
  }

  lazy val baseConfig = ConfigFactory.load //defaults from src/resources

  val systemConfig = Option(System.getProperty("concrete.config")) match {
    case Some(cfile) => ConfigFactory.parseFile(new File(cfile)).withFallback(baseConfig)
    case None        => baseConfig
  }

  lazy val DB = Database.forConfig("database", systemConfig)

  val statistic :: version :: nature = args.toList

  // Set display :
  // update "Problem" set display = substring(name from '%/#"[^/]+#".xml.bz2' for '#')

  case class Problem(
    problemId: Int,
    problem: String,
    nbVars: Int,
    nbCons: Int,
    tags: Array[String])

  case class Config(
      configId: Int,
      config: Node) {
    def display = config \\ "p" map (n => className(n)) mkString ("/")
    override def toString = configId + "." + display
  }

  case class Resultat[A](
      status: String,
      statistic: A) {
    def solved: Failure = {
      if (status == "SAT" || status == "UNSAT") {
        Success
      } else if (status.contains("Timeout")) {
        Timeout
      } else if (status.contains("OutOfMemory") || status.contains("relation is too large")) {
        OOM
      } else {
        UnknownError
      }
    }
  }

  sealed trait Failure
  case object Success extends Failure
  case object Timeout extends Failure
  case object OOM extends Failure
  case object UnknownError extends Failure

  implicit val getProblemResult = GetResult(r => Problem(r.<<, r.<<, r.<<, r.<<, r.nextStringOption.map(_.split(",")).getOrElse(Array())))

  implicit val getConfigResult = GetResult(r =>
    Config(r.<<, {
      val s = r.nextString;
      try xml.XML.loadString(s"<config>$s</config>")
      catch {
        case e: Exception =>
          System.err.println(s)
          throw e
      }
    }))

  val problemsQ = sql"""
        SELECT "problemId", display, "nbVars", "nbCons", string_agg("problemTag", ',') as tags
        FROM "Problem" NATURAL LEFT JOIN "ProblemTag"
        WHERE "problemId" IN (
          SELECT "problemId" 
          FROM "Execution"
          WHERE version = $version and "configId" in (#${nature.mkString(",")}))
          -- AND "problemTag" = 'modifiedRenault'
        GROUP BY "problemId", display, "nbVars", "nbCons"
        ORDER BY display
        """.as[Problem]

  val configsQ = sql"""
        SELECT "configId", config
        FROM "Config" 
        WHERE "configId" IN (#${nature.mkString(", ")}) 
        """.as[Config]

  val problems = Await.result(DB.run(problemsQ), Duration.Inf)
  println("problems")
  val configs = Await.result(DB.run(configsQ), Duration.Inf)

  for (c <- configs) print("\t" + c.display)
  println()

  val cIds = configs.map(_.configId).mkString(", ")

  //var d = Array.ofDim[Int](configs.size, configs.size)

  val totals = new HashMap[(String, Int), List[Double]]().withDefaultValue(Nil)

  val nbSolved = new HashMap[String, Array[Int]]().withDefaultValue {
    Array.fill(configs.size)(0)
  }

  val timeoutHandler: ErrorHandling = statistic match {
    case "rps"          => ErrorKeep
    case "nodes"        => ErrorInfinity
    case "time"         => ErrorInfinity
    case "revisions"    => ErrorInfinity
    case "mem"          => ErrorKeep
    case "domainChecks" => ErrorInfinity
    case "nps"          => ErrorKeep
  }

  val oomHandler: ErrorHandling = statistic match {
    case "rps"          => ErrorZero
    case "nodes"        => ErrorInfinity
    case "time"         => ErrorInfinity
    case "revisions"    => ErrorInfinity
    case "mem"          => ErrorInfinity
    case "domainChecks" => ErrorInfinity
    case "nps"          => ErrorZero
  }

  // val ignoreNaN = true

  val aggregator = {
    data: Seq[Double] => if (data.isEmpty) -1 else StatisticsManager.median[Double](data)
  }

  for (Problem(problemId, problem, nbvars, nbcons, tags) <- problems; it <- 0 until 5) {

    val data = ListBuffer(s"$problem ($problemId), it $it") //, nbvars, nbcons)
    //print("\\em %s & \\np{%d} & \\np{%d}".format(problem, nbvars, nbcons))
    //print("\\em %s ".format(problem))

    //val name = List("solver.searchCpu", "filter.revisions", "solver.nbAssignments", "filter.substats.queue.pollSize")(3)

    //val formula = """(cast("filter.substats.queue.pollSize" as real) / cast("filter.substats.queue.nbPoll" as int))"""

    //val formula = """cast("solver.nbAssignments" as real) / cast("solver.searchCpu" as real)"""
    ///val formula = """cast("solver.nbAssignments" as real)"""
    //val formula = """cast("solver.nbAssignments" as real) / (cast("solver.searchCpu" as real) + cast("solver.preproCpu" as real))"""
    //val formula = """cast("solver.searchCpu" as real) + cast("solver.preproCpu" as real)"""

    //val formula = """cast("relation.checks" as bigint)"""
    //val formula = """cast("concrete.generationTime" as real)"""

    //val formula = """cast("domains.presenceChecks" as bigint)"""

    //val stat = "cast(stat('solver.nbAssignments', executionId) as int)"

    //val stat = "cast(stat('solver.searchCpu', executionId) as real) + cast(stat('solver.preproCpu', executionId) as real)"

    //val min = true

    //            val sqlQuery = """
    //                        SELECT configId, solution, cast(stat('relation.checks', executionId) as real)
    //                        FROM Executions
    //                        WHERE (version, problemId) = (%d, %d)
    //                    """.format(version, problemId)

    val sqlQuery = statistic match {
      case "mem" => sql"""
                                SELECT "configId", status, solution, cast(stat('solver.usedMem', "executionId") as real)/1048576.0
                                FROM "Execution"
                                WHERE (version, "problemId", iteration) = ($version, $problemId, $it)
                            """

      case "time" => sql"""
                                SELECT "configId", status, solution, totalTime('{solver.searchCpu, solver.preproCpu}', "executionId")/1e3
                                FROM "Execution"
                                WHERE (version, "problemId", iteration) = ($version, $problemId, $it)
                            """

      case "nodes" => sql"""
                                SELECT "configId", status, solution, cast(stat('solver.nbAssignments', "executionId") as real)
                                FROM "Execution"
                                WHERE (version, "problemId", iteration) = ($version, $problemId, $it)
                            """
      case "domainChecks" => sql"""
                                SELECT "configId", status, solution, cast(stat('solver.domain.checks', "executionId") as real)
                                FROM "Execution"
                                WHERE (version, "problemId", iteration) = ($version, $problemId, $it)
                            """
      case "nps" => sql"""
                                SELECT "configId", status, solution, 
                                  cast(stat('solver.nbAssignments', "executionId") as real)/totalTime('{solver.searchCpu, solver.preproCpu}', "executionId")*1e3
                                FROM "Execution"
                                WHERE (version, "problemId", iteration) = ($version, $problemId, $it)"""
      case "rps" => sql"""
                                SELECT "configId", status, solution, 
                                  cast(stat('solver.filter.revisions', "executionId") as real)/totalTime('{solver.searchCpu, solver.preproCpu}', "executionId")*1e3
                                FROM "Execution"
                                WHERE (version, "problemId", iteration) = ($version, $problemId, $it)"""

      case "revisions" => sql"""
                                SELECT "configId", status, solution, 
                                  cast(stat('solver.filter.revisions', "executionId") as bigint)
                                FROM "Execution"
                                WHERE (version, "problemId", iteration) = ($version, $problemId, $it)"""

    }

    val resultsQ = DB.run(sqlQuery.as[(Int, String, Option[String], Option[Double])])
      .map {
        _.map {
          case (config, status, solution, value) =>
            config -> Resultat(status, value.getOrElse(Double.NaN))
        }
          .toMap
      }

    val trie = Try(Await.result(resultsQ, Duration.Inf))
      .recoverWith {
        case e: Exception =>
          e.printStackTrace()
          Failure(e)
      }

    print(s"$problem $it\t")
    for {
      results <- trie
      i <- configs.indices
      j = results.getOrElse(configs(i).configId, Resultat("not started", Double.NaN))
    } {
      val stat = j.solved match {
        case Success =>
          for (tag <- tags)
            nbSolved(tag)(i) += 1
          j.statistic
        case Timeout =>
          // println(s"$problemId. $problem, ${configs(i)}: $j")
          timeoutHandler.toDouble(j.statistic)
        case OOM =>
          oomHandler.toDouble(j.statistic)
        case _ =>
          //println(s"$problemId. $problem, ${configs(i)}: $j")
          print(s"$j:")
          Double.NaN
      }
      print(s"$stat\t")
      for (tag <- tags)
        totals((tag, i)) ::= stat
    }
    println()

    //    for (
    //      i <- configs.indices; j <- configs.indices if i != j;
    //      ci = configs(i).configId;
    //      cj = configs(j).configId;
    //      ri <- results.get(ci) if ri.solved;
    //      rj <- results.get(cj)
    //    ) {
    //      if (!rj.solved || {
    //        val trj = rj.statistic
    //        val tri = ri.statistic
    //        (trj - tri) / tri > .1 && (trj - tri) > 1
    //      }) {
    //        d(i)(j) += 1
    //      }
    //    }

    //      val extrem = results.values.map(_._2).toSeq match {
    //        case Nil => None
    //        case d: Seq[Double] => Some(if (min) d.min else d.max)
    //      }

    //    configs foreach { c =>
    //      data.append(
    //        results.get(c.configId) match {
    //          case Some(r @ Resultat(status, solution, time)) =>
    //            val e = engineer(time)
    //            "%.1f%s".formatLocal(Locale.US, e._1, e._2.getOrElse("")) + (
    //              if (r.solved) {
    //                ""
    //              } else if (solution.contains("OutOfMemoryError")) {
    //                "(mem)"
    //              } else if (solution.contains("InterruptedException")) {
    //                "(exp)"
    //              } else {
    //                s"(${status})"
    //              })
    //          case None => "---"
    //        })
    //    }

    //println(tabular(data.toSeq))

    // println("\\midrule")

    //    for ((k, t) <- totals.toList.sortBy(_._1)) {
    //      println(k + " : " + configs.indices.map { i =>
    //        t(i)
    //      }.mkString(" & "))
    //    }

    //println(totals)
  }

  val tagged = totals.groupBy(_._1._1).map {
    case (tag, tot) => tag -> tot.map {
      case ((tag, config), values) => config -> values
    }
  }

  for ((k, t) <- tagged.toList.sortBy(_._1)) {
    //println(s"$k: $t")
    val medians = configs.indices.map {
      i =>
        //if (ignoreNaN) {
        //  aggregator(t(i).filterNot(_.isNaN))
        //} else 
        if (t(i).exists(_.isNaN)) {
          Double.NaN
        } else {
          try aggregator(t(i))
          catch {
            case e: NoSuchElementException => Double.NaN
          }
        }

    }

    println(s"$k," + medians.map { median =>
      median.toString
      //            val (v, m) = engineer(median)
      //
      //            (if (median < best * 1.1) "\\bf " else "") + (
      //              m match {
      //                case Some(m) => f"\\np[$m%s]{$v%.1f}"
      //                case None => f"\\np{$v%.1f}"
      //              })
    }.mkString(",")) // + " \\\\")

  }

  //println("\\midrule")

  //      for ((k, counts) <- nbSolved.toList.sortBy(_._1)) {
  //        val best = counts.max
  //
  //        println(s"\\em $k & " + counts.map {
  //          i => if (i == best) s"\\bf $i" else s"$i"
  //        }.mkString(" & ") + " \\\\")
  //      }

  //    println(d.zipWithIndex map { case (r, i) => configs(i) + " " + r.mkString(" ") } mkString ("\n"))
  //    println()

  //      val labels = configs.map(_.toString).toIndexedSeq
  //
  //      toGML(d, labels)
  //
  //      val s = schulze(winnerTakesAll(d))
  //
  //      println(rank(s, s.indices).toList.sortBy(_._1) map {
  //        case (r, c) => "%d: %s".format(r, c.map(labels).mkString(" "))
  //      } mkString ("\n"))

  // solution != null && ("UNSAT" == solution || """^[0-9\s]*$""".r.findFirstIn(solution).isDefined || "^Map".r.findFirstIn(solution).isDefined)

  @tailrec
  def rank[B <% Ordered[B]](p: IndexedSeq[Array[B]], candidates: Seq[Int],
                            cRank: Int = 1, ranking: SortedMap[Int, Seq[Int]] = SortedMap.empty): SortedMap[Int, Seq[Int]] =
    if (candidates.isEmpty) {
      ranking
    } else {
      val (win, rem) = candidates.partition { i =>
        candidates.forall { j => p(i)(j) >= p(j)(i) }
      }
      if (win.isEmpty) ranking
      else {
        //println(cRank + " : " + win)
        rank(p, rem, cRank + win.size, ranking + (cRank -> win))
      }
    }

  def winnerTakesAll(d: Array[Array[Int]]) = {
    val p = Array.ofDim[Int](d.length, d.length)
    for (i <- d.indices.par; j <- d.indices) {
      if (d(i)(j) > d(j)(i)) {
        p(i)(j) = d(i)(j)
      }
    }
    p
  }

  def schulze[A <% Ordered[A]](p0: Array[Array[A]]) = {
    //val p = percentages(d, avis)

    val p = p0.map(_.clone)

    for (i <- p.indices) {
      //println("%.0f %%".format(100.0 * i / p.length))

      for (j <- p.indices if i != j) {
        var k = 0;
        while (k < p.length) {
          if (i != k && j != k) {
            p(j)(k) = max(p(j)(k), min(p(j)(i), p(i)(k)))
          }
          k += 1
        }
      }
    }

    //p.map(_.mkString(" ")).foreach(println)

    p

    //toGML(d,labels)
  }

  private def min[A <% Ordered[A]](a: A, b: A): A =
    if (a < b) a else b

  private def max[A <% Ordered[A]](a: A, b: A): A =
    if (a > b) a else b

  private def toGML(p: Array[Array[Int]], labels: IndexedSeq[String]) {
    println("graph [ directed 0 ");
    for (c <- p.indices) {
      println("""node [ id %d label "%s" ]""".format(c, labels(c)))
    }

    val s = """edge [ source %d target %d label "%d" graphics [ targetArrow "standard" ] ] """

    for (i <- p.indices; j <- p.indices if (i != j)) {
      if (p(i)(j) > 0) {
        println(s.format(i, j, p(i)(j)))
      }
    }

    println("]")
  }

  def className(n: NodeSeq) = {
    n.headOption match {
      case Some(conf) => conf.text.split('.').last
      case None       => "?"
    }
  }

  def engineer(value: Double): (Double, Option[Char]) = {
    if (value == 0 || value.isInfinity) { (value, None) }
    else {
      val CONSTANTS = Map(
        3 -> Some('G'),
        2 -> Some('M'),
        1 -> Some('k'),
        0 -> None,
        -1 -> Some('m'),
        -2 -> Some('u'),
        -3 -> Some('n'));

      val level = math.floor(math.log10(math.abs(value)) / 3).toInt;

      (value * math.pow(10, -3 * level), CONSTANTS(level));
    }
  }

}
