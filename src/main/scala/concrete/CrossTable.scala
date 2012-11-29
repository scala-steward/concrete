package concrete

import java.net.URI
import SQLWriter._
import scala.xml.Node
import scala.xml.Text
import scala.xml.NodeSeq
import scala.collection.mutable.ListBuffer
import cspfj.StatisticsManager
import java.util.Locale

object CrossTable extends App {

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
          case d: Int => "\\np{%d}".format(d)
          case _ => "---"
        }.mkString(" & ") + "\\\\"

      case "csv" => data.mkString(", ")

    }
  }

  //3647557
  val version = args.head.toInt //1888

  //  val acv = List(98, 96, 95, 97, 99)
  //  val acc = List(103, 101, 100, 102, 104)
  //
  //  val heur = List(85, 64, 83, 84, 70, 72, 59, 71, 87, 86)

  //val nature = List(95, 100)
  val nature = args.tail map (_.toInt) //6 to 10//List(1, 2, 3, 4, 5)

  using(SQLWriter.connect(new URI("postgresql://concrete:concrete@localhost/concrete"))) { connection =>

    val problems = queryEach(connection, """
        SELECT DISTINCT problemId, display, nbvars, nbcons
        FROM executions NATURAL JOIN problems
        WHERE version = %d and configId in (%s)
        ORDER BY display
        """.format(version, nature.mkString(", "))) {
      rs =>
        (rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4))
    }

    val configs = queryEach(connection, """
        SELECT configId, config
        FROM configs 
        WHERE configId IN (%s) 
        """.format(nature.mkString(", "))) {
      rs => (rs.getInt(1), configDisplay(xml.XML.load(rs.getSQLXML(2).getBinaryStream), rs.getInt(1)))
    } // sortBy (_._2)

    for ((_, desc) <- configs) print(" & " + desc)
    println("\\\\")
    println("\\midrule")

    val cIds = configs map (_._1) mkString (", ")

    var d = Array.ofDim[Int](configs.size, configs.size)

    //    val statistics = queryEach(connection, """
    //            SELECT DISTINCT name 
    //            FROM Statistics NATURAL JOIN Executions
    //            WHERE version = %d AND configId IN (%s)
    //            GROUP BY name, configId
    //            HAVING count(*) = %d
    //    		ORDER BY name
    //        """.format(version, cIds, cIds.length))(_.getString(1))
    //
    //    require(statistics.nonEmpty)

    val statistics = Seq("solver.searchCpu", "solver.preproCpu", "solver.nbAssignments")

    val totals = Array.fill[List[Double]](configs.size)(Nil)

    for ((problemId, problem, nbvars, nbcons) <- problems) {

      val data = ListBuffer(problem) //, nbvars, nbcons)
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

      val stat = "cast(stat('solver.searchCpu', executionId) as real) + cast(stat('solver.preproCpu', executionId) as real)"

      val min = true

      val sqlQuery = """
            SELECT configId, solution, (%s)
            FROM Executions
            WHERE (version, problemId) = (%d, %d)
        """.format(stat, version, problemId)

      //println(sqlQuery)
      val results = queryEach(connection, sqlQuery) {
        rs => rs.getInt(1) -> (rs.getString(2), rs.getDouble(3))
      } toMap

      for (
        i <- configs.indices;
        j <- results.get(configs(i)._1);
        val k = if (solved(j._1)) j._2 else Double.PositiveInfinity
      ) {
        totals(i) ::= k
      }

      for (
        i <- configs.indices; j <- configs.indices if i != j;
        val ci = configs(i)._1;
        val cj = configs(j)._1;
        ri <- results.get(ci) if solved(ri._1);
        rj <- results.get(cj)
      ) {
        if (solved(rj._1)) {
          val trj = rj._2
          val tri = ri._2
          if ((trj - tri) / math.min(tri, trj) > .1 && (trj - tri) > 1) d(i)(j) += 1
        } else d(i)(j) += 1
      }

      //      val extrem = results.values.map(_._2).toSeq match {
      //        case Nil => None
      //        case d: Seq[Double] => Some(if (min) d.min else d.max)
      //      }

      configs foreach { c =>
        data.append(
          results.get(c._1) match {
            case Some((result, time)) =>
              if (solved(result)) {
                //engineer(e)._1
                //        if (e._2 != "M") throw new IllegalStateException
                //print(" & ")
                //if (extrem.isDefined && (if (min) e < extrem.get * 1.1 else e > extrem.get * .9)) print("\\bf")
                val e = engineer(time)
                "%.1f%s".formatLocal(Locale.US, e._1, e._2.getOrElse("")) //print("\\np{%.1f}".format(r))
              } else {

                if (result == null) "null"
                else if (result.contains("OutOfMemoryError")) "mem"
                else if (result.contains("InterruptedException")) "exp"
                else result
              }
            case None => "---"
          })
      }

      println(tabular(data.toSeq))
    }

    println("\\midrule")

    println(configs.indices.map { i =>
      val e = engineer(StatisticsManager.median(totals(i)))
      "%.1f%s".formatLocal(Locale.US, e._1, e._2.getOrElse(""))
    }.mkString(" & "))

    println(configs.indices.map { i => 
      "%d".format(totals(i).count(!_.isInfinity))
    }.mkString(" & "))

    println(d.zipWithIndex map { case (r, i) => configs(i) + " " + r.mkString(" ") } mkString ("\n"))
    println()

    schulze(d, configs.map(c => c._1 + "." + c._2).toIndexedSeq)

  }

  def solved(solution: String) = solution != null && ("UNSAT" == solution || """^[0-9\ ]*$""".r.findFirstIn(solution).isDefined)

  def rank(p: Array[Array[Int]], candidates: Seq[Int], cRank: Int = 1, ranking: Map[Int, Seq[Int]] = Map.empty): Map[Int, Seq[Int]] =
    if (candidates.isEmpty) ranking
    else {
      val (win, rem) = candidates.partition { i =>
        candidates.forall { j => p(i)(j) >= p(j)(i) }
      }
      rank(p, rem, cRank + win.size, ranking + (cRank -> win))
    }

  def schulze(d: Array[Array[Int]], labels: IndexedSeq[String]) {

    val p = Array.ofDim[Int](d.size, d.size)

    for (i <- d.indices; j <- d.indices if (i != j)) {
      p(i)(j) =
        if (d(i)(j) > d(j)(i)) d(i)(j) - d(j)(i)
        else 0 //Int.MaxValue
    }

    //    println(p map (_.mkString(" ")) mkString ("\n"))
    //    println()

    for (i <- d.indices; j <- d.indices; k <- d.indices) {
      p(i)(j) = math.max(p(i)(j), math.min(p(i)(k), p(k)(j)))
    }

    //    println(p map (_.mkString(" ")) mkString ("\n"))
    //    println()

    println(rank(p, p.indices).toList.sortBy(_._1) map {
      case (r, c) => "%d: %s".format(r, c.map(labels).mkString(" "))
    } mkString ("\n"))
    //
    //    var candidates: Set[Int] = d.indices.toSet
    //
    //    var rank = 1
    //    while (candidates.nonEmpty) {
    //      print(rank + ". ")
    //      val (win, rem) = candidates.partition { i =>
    //        candidates.forall { j => p(i)(j) >= p(j)(i) }
    //      }
    //      println(win.map(labels).mkString(" "))
    //      rank += win.size
    //      candidates = rem
    //    }

    //    println("graph [ directed 0 ");
    //    for (c <- d.indices) {
    //      println("""node [ id %d label "%s" ]""".format(c, labels(c)))
    //    }
    //
    //    val s = """edge [ source %d target %d label "%d" graphics [ targetArrow "standard" ] ] """
    //
    //    for (i <- d.indices; j <- d.indices if (i != j)) {
    //      if (p(i)(j) > 0) {
    //        println(s.format(i, j, p(i)(j)))
    //      }
    //    }
    //
    //    println("]")
  }

  def configDisplay(desc: NodeSeq, id: Int) = {
    desc \\ "p" map (n => className(n)) mkString ("/")
    //    val filter = className(desc \\ "p" filter attributeEquals("name", "mac.filter"))
    //
    //    val queue = filter match {
    //      case "ACC" | "AC3Constraint" => className(desc \\ "p" filter attributeEquals("name", "ac3c.queue"))
    //      case "ACV" | "AC3" => className(desc \\ "p" filter attributeEquals("name", "ac3v.queue"))
    //      case _ => sys.error("Unknown filter: " + filter)
    //    }
    //
    //    val heur = filter match {
    //      case "ACC" if queue != "QuickFifo" && queue != "SimpleFifos" && queue != "JavaFifo" && queue != "JavaSimpleFifos" =>
    //        className(desc \\ "p" filter attributeEquals("name", "ac3c.key"))
    //      case "ACV" if queue != "QuickFifo" =>
    //        className(desc \\ "p" filter attributeEquals("name", "ac3v.key"))
    //      case _ => "na"
    //    }
    //
    //    val reduction = className(desc \\ "p" filter attributeEquals("name", "relationAlgorithm"))
    //    val relation = className(desc \\ "p" filter attributeEquals("name", "relationStructure"))
    //
    //    "%s-%s-%s-%s-%s (%d)".format(filter, queue, heur, reduction, relation, id)
  }

  def className(n: NodeSeq) = {
    n.headOption match {
      case Some(conf) => conf.text.split('.').last
      case None => "?"
    }
  }

  def engineer(value: Double): (Double, Option[Char]) = {
    if (value == 0 || value.isInfinity) (value, None)
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
