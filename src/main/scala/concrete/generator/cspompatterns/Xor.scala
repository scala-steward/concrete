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
import cspom.variable.CSPOMSeq
import CSPOM._

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

  override def matchBool(c: CSPOMConstraint[_], p: CSPOM) = {
    c.function == 'xor && !c.result.isTrue && c.arguments.length == 2
  }

  def compile(fc: CSPOMConstraint[_], problem: CSPOM) = {

    val Seq(a: SimpleExpression[_], b: SimpleExpression[_]) = fc.arguments
    val res = fc.result

    val alt1 = CSPOMConstraint(new BoolVariable())('clause)(Seq(a, b), Seq[SimpleExpression[Boolean]]())
    val alt2 = CSPOMConstraint(new BoolVariable())('clause)(Seq[SimpleExpression[Boolean]](), Seq(a, b))
    val conj = CSPOMConstraint(res)('and)(alt1.result, alt2.result)

    replaceCtr(fc, Seq(alt1, alt2, conj), problem)

  }

  def selfPropagation = false

}
