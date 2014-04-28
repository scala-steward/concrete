package concrete.generator.cspompatterns

import cspom.CSPOM
import scala.collection.mutable.Queue
import cspom.compiler.ConstraintCompiler
import cspom.CSPOMConstraint
import cspom.variable.CSPOMVariable
import cspom.compiler.Delta
import cspom.variable.BoolVariable
import cspom.variable.CSPOMConstant
import cspom.variable.CSPOMConstant
import cspom.compiler.ConstraintCompilerNoData
import cspom.variable.CSPOMExpression
import cspom.variable.SimpleExpression

/**
 * Reified conjunction is converted to CNF :
 *
 * a = b (+) c
 *
 * <=>
 *
 * a
 */
object Xor extends ConstraintCompilerNoData {

  override def matchBool(c: CSPOMConstraint[_], p: CSPOM) = c.function == 'xor

  def compile(fc: CSPOMConstraint[_], problem: CSPOM) = {

    val Seq(a: SimpleExpression[Boolean], b: SimpleExpression[Boolean]) = fc.arguments
    val res = fc.result.asInstanceOf[SimpleExpression[Boolean]]

    val alt1 = CSPOMConstraint(new BoolVariable(), 'or, Seq(a, b))
    val alt2 = CSPOMConstraint(new BoolVariable(), 'or, Seq(a, b), Map("revsign" -> Seq(true, true)))
    val conj = CSPOMConstraint(res, 'and, Seq(alt1.result, alt2.result))

    replaceCtr(fc, Seq(alt1, alt2, conj), problem)

  }

  def selfPropagation = false

}