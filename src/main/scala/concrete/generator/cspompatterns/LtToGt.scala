package concrete.generator.cspompatterns

import cspom.CSPOM
import scala.collection.mutable.Queue
import cspom.compiler.ConstraintCompiler
import cspom.CSPOMConstraint
import cspom.variable.CSPOMVariable
import cspom.compiler.Delta
import cspom.variable.BoolVariable
import cspom.variable.CSPOMTrue
import cspom.variable.CSPOMConstant
import cspom.compiler.ConstraintCompilerNoData
import cspom.variable.BoolExpression
import cspom.variable.CSPOMFalse
import cspom.variable.IntExpression
import cspom.variable.CSPOMExpression

/**
 * Only one side of comparison is supported, so :
 *
 * a = b < c <=> a = c > b
 *
 * and
 *
 * a = b <= c <=> a = c >= b
 */
object LtToGt extends ConstraintCompiler {

  type A = Boolean

  def mtch(fc: CSPOMConstraint, problem: CSPOM) = fc.function match {
    case 'lt => Some(true)

    case 'le => Some(false)

    case _ => None

  }

  def compile(fc: CSPOMConstraint, problem: CSPOM, strict: Boolean) = {
    replaceCtr(fc,
      new CSPOMConstraint(fc.result, (if (strict) 'gt else 'ge), fc.arguments.reverse, fc.params),
      problem)

  }

}