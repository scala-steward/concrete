package cspfj.generator.constraint;

import cspfj.constraint.extension.ExtensionConstraint
import cspfj.constraint.extension.Matrix
import cspfj.constraint.extension.Matrix2D
import cspfj.constraint.extension.MatrixGeneral
import cspfj.Domain
import cspfj.Problem
import cspfj.Variable
import cspom.constraint.CSPOMConstraint
import cspom.extension.Relation
import cspfj.constraint.extension.TupleHashSet

final case class Signature(domains: Seq[Domain], relation: Relation, init: Boolean)

final class ExtensionGenerator(problem: Problem) extends AbstractGenerator(problem) {

  var generated: Map[Signature, Matrix] = Map.empty

  private def generate(variables: Seq[Variable], relation: Relation, init: Boolean) = {
    val domains = variables map (_.dom)

    val signature = Signature(domains, relation, init);
    generated.get(signature) match {
      case None => {
        val matrix = ExtensionGenerator.bestMatrix(relation, init, domains map (_.size))
        ExtensionGenerator.fillMatrix(domains, relation, init, matrix)
        generated += signature -> matrix
        matrix
      }
      case Some(m) => m
    }

  }

  def generate(constraint: CSPOMConstraint) = {
    require(constraint.isInstanceOf[cspom.extension.ExtensionConstraint])

    val solverVariables = constraint.scope map cspom2cspfj

    if (solverVariables exists (_.dom == null)) {
      false
    } else {
      val extensionConstraint = constraint.asInstanceOf[cspom.extension.ExtensionConstraint]
      val matrix = generate(solverVariables, extensionConstraint.relation, extensionConstraint.init);

      addConstraint(ExtensionConstraint.newExtensionConstraint(matrix, solverVariables.toArray));
      true;
    }
  }
}

object ExtensionGenerator {
  val TIGHTNESS_LIMIT = 4;

  def tupleSetBetterThanMatrix(sizes: Seq[Int], nbTuples: Int) = {
    val size = sizes.foldLeft(BigInt(1))(_ * _)
    size > Int.MaxValue || size > (TIGHTNESS_LIMIT * nbTuples)
  }

  def bestMatrix(relation: Relation, init: Boolean, sizes: Seq[Int]) = {
    if (relation.arity == 2) {
      new Matrix2D(sizes(0), sizes(1), init);
    } else if (!init && tupleSetBetterThanMatrix(sizes, relation.size)) {
      new TupleHashSet(relation.size, init);
    } else {
      new MatrixGeneral(sizes.toArray, init);
    }
  }

  def fillMatrix(domains: Seq[Domain], relation: Relation, init: Boolean, matrix: Matrix) {

    for (values <- relation.iterator map { _.asInstanceOf[Seq[Int]] }) {
      val tuple = (values, domains).zipped.map { (v, d) => d.index(v) }
      matrix.set(tuple.toArray, !init)
    }

  }
}
