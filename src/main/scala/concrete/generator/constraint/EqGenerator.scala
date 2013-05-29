package concrete.generator.constraint;

import concrete.constraint.semantic.{ ReifiedConstraint, Neq, Eq }
import concrete.generator.FailedGenerationException
import concrete.{ Variable, Problem, Domain, IntDomain }
import cspom.constraint.{ GeneralConstraint, FunctionalConstraint, CSPOMConstraint }
import concrete.BooleanDomain
import concrete.constraint.Constraint
import concrete.constraint.semantic.Disjunction

final class EqGenerator(problem: Problem) extends AbstractGenerator(problem) {

  override def generateGeneral(constraint: GeneralConstraint): Boolean = {
    require(Seq("=", "eq").contains(constraint.description));
    val scope = constraint.scope.map(cspom2concrete)

    scope.map(_.dom).find(!_.undefined) match {
      case None => false
      case Some(refDomain: IntDomain) =>
        generateInt(refDomain, scope); true
      case Some(refDomain: BooleanDomain) =>
        generateBool(refDomain, scope); true
      case _ => throw new FailedGenerationException("Unhandled domain")
    }
  }

  private def generateInt(refDomain: IntDomain, scope: Seq[Variable]) {
    for (v <- scope if (v.dom.undefined)) {
      v.dom = IntDomain(refDomain.values.toSeq: _*)
    }
    for (Seq(v0, v1) <- scope.sliding(2)) {
      addConstraint(new Eq(v0, v1));
    }
  }

  private def generateBool(refDomain: BooleanDomain, scope: Seq[Variable]) {
    scope filter { _.dom.undefined } map { _.dom.asInstanceOf[BooleanDomain] } find { _.size == 1 } match {
      case None =>
        for (v <- scope if (v.dom.undefined)) {
          v.dom = new BooleanDomain(refDomain.status)
        }

        for (Seq(i, j) <- scope.combinations(2)) {
          addConstraint(new Disjunction(Array(i, j), Array(false, true)))
        }

      case Some(constantDomain) =>
        for (v <- scope) {
          if (v.dom.undefined) {
            v.dom = new BooleanDomain(refDomain.status)
          } else {
            val d = v.dom.asInstanceOf[BooleanDomain]
            if (d.isUnknown) {
              d.status = refDomain.status
            } else if (d.status != refDomain.status) {
              throw new FailedGenerationException("Inconsistent equality")
            }
          }
        }

    }

  }

  override def generateFunctional(funcConstraint: FunctionalConstraint) = funcConstraint.description match {
    case "eq" => {
      val arguments = funcConstraint.arguments map cspom2concrete

      if (arguments.size != 2 || arguments.exists(_.dom.undefined)) {
        false
      } else {
        val result = cspom2concrete(funcConstraint.result)
        AbstractGenerator.booleanDomain(result);
        addConstraint(
          new ReifiedConstraint(
            result,
            new Eq(arguments(0), arguments(1)),
            new Neq(arguments(0), arguments(1))))
        true
      }
    }
    case "neg" =>
      if (funcConstraint.scope.size != 2) {
        false
      } else {
        val scope = funcConstraint.scope map cspom2concrete

        val refDomain = scope map { _.dom } find { !_.undefined }

        if (refDomain == None) {
          false
        } else {
          val negDomain = refDomain.get.values.map(v => -v).toSeq.reverse

          for (v <- scope if (v.dom.undefined)) {
            v.dom = IntDomain(negDomain: _*)
          }
          addConstraint(new Eq(true, scope(0), 0, scope(1)))
          true
        }
      }

    case _ =>
      throw new IllegalArgumentException("Can not generate " + funcConstraint);
  }

}
