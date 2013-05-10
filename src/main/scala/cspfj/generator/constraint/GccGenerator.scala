package cspfj.generator.constraint;

import cspfj.constraint.semantic.Bounds
import cspfj.constraint.semantic.Gcc
import cspfj.{ Variable, Problem }
import cspom.constraint.{ GeneralConstraint, CSPOMConstraint }

final class GccGenerator(problem: Problem) extends AbstractGenerator(problem) {
  override def generateGeneral(constraint: GeneralConstraint) = {
    val scope = constraint.scope map cspom2cspfj

    if (scope exists (_.dom == null)) {
      false
    } else {
      val bounds = constraint.predicate.parameters match {
        case Some(p: Seq[(Int, Int, Int)]) => p.map(i => Bounds(i._1, i._2, i._3)).toArray
        case Some(p: Array[(Int, Int, Int)]) => p.map(i => Bounds(i._1, i._2, i._3))
        case p: Any => throw new IllegalArgumentException(s"GCC constraints requires to be parameterized with a sequence of triples, found $p")
      }

      addConstraint(new Gcc(scope.toArray, bounds));
      true;
    }

  }
}
