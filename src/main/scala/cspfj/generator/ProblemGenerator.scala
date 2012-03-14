package cspfj.generator;

import cspfj.generator.constraint.GeneratorManager
import cspfj.problem.{ Domain, IntDomain, Problem }
import cspom.constraint.CSPOMConstraint
import cspom.variable.{ CSPOMDomain, BooleanDomain }
import cspom.CSPOM
import scala.annotation.tailrec
import scala.collection.immutable.Queue
import cspom.variable.IntInterval

object ProblemGenerator {
  @throws(classOf[FailedGenerationException])
  def generate(cspom: CSPOM) = {

    // new ProblemCompiler(cspom).compile();

    val problem = new Problem();

    generateVariables(problem, cspom)

    val gm = new GeneratorManager(problem);

    var firstFailed: Option[CSPOMConstraint] = None;

    @tailrec
    def processQueue(queue: Queue[CSPOMConstraint]): Unit = if (queue.nonEmpty) {
      val (constraint, rest) = queue.dequeue

      if (gm.generate(constraint)) {
        firstFailed = None;
        processQueue(rest)
      } else firstFailed match {
        case Some(c) if c == constraint =>
          throw new FailedGenerationException(
            "Could not generate the constraints " + queue);
        case Some(_) =>
          processQueue(rest.enqueue(constraint));
        case None =>
          firstFailed = Some(constraint);
          processQueue(rest.enqueue(constraint));
      }

    }

    processQueue(Queue.empty ++ cspom.constraints)
    problem;
  }

  def generateVariables(problem: Problem, cspom: CSPOM) {
    for (v <- cspom.variables) {
      problem.addVariable(v.name, generateDomain(v.domain));
    }
  }

  def generateDomain[T](cspomDomain: CSPOMDomain[T]): Domain = cspomDomain match {
    case null => null
    case bD: BooleanDomain =>
      if (bD.isConstant) {
        new cspfj.problem.BooleanDomain(bD.getBoolean);
      } else {
        new cspfj.problem.BooleanDomain();
      }
    case int: IntInterval => new IntDomain(int.lb, int.ub)

    case ext: CSPOMDomain[Int] => new IntDomain(ext.values)

    case _ => throw new UnsupportedOperationException
  }
}
