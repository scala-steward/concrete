package concrete.generator.constraint;

import concrete.generator.FailedGenerationException
import concrete.Problem
import cspom.CSPOMConstraint
import scala.collection.mutable.HashMap
import concrete.constraint.Constraint
import cspom.variable.CSPOMVariable
import concrete.Variable
import concrete.ParameterManager
import cspom.CSPOM
import cspom.VariableNames
import scala.util.Try
import scala.util.Failure
import scala.util.Success

class GeneratorManager(pm: ParameterManager) {
  private var known: Map[Symbol, Generator] = Map(
    'abs -> AbsGenerator,
    'alldifferent -> AllDifferentGenerator,
    'eq -> EqGenerator,
    //'gt -> GtGenerator,
    //'ge -> GtGenerator,
    'mul -> MulGenerator,
    'absdiff -> AbsDiffGenerator,
    'gcc -> GccGenerator,
    'div -> DivGenerator,
    'mod -> ModGenerator,
    'nevec -> NeqVecGenerator,
    'sum -> new SumGenerator(pm),
    'pseudoboolean -> new SumGenerator(pm),
    'lexleq -> LexLeqGenerator,
    'occurrence -> OccurrenceGenerator,
    'extension -> new ExtensionGenerator(pm),
    'sq -> SquareGenerator,
    'min -> MinGenerator,
    'max -> MaxGenerator,
    'element -> ElementGenerator,
    'in -> SetInGenerator)

  def register(entry: (Symbol, Generator)) {
    known += entry
  }

  def generate[A](constraint: CSPOMConstraint[A], variables: Map[CSPOMVariable[_], Variable], vn: VariableNames): Try[Seq[Constraint]] = {
    known.get(constraint.function).map(Success(_))
      .getOrElse(Failure(new FailedGenerationException(s"No candidate constraint for $constraint")))
      .flatMap { candidate =>
        Try {
          candidate.generate(constraint, variables: Map[CSPOMVariable[_], Variable])
        }
          .recoverWith {
            case e =>
              Failure(new FailedGenerationException("Failed to generate " + constraint.toString(vn), e))
          }
      }
  }

}
