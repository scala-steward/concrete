package concrete.generator.cspompatterns

import cspom.CSPOM
import cspom.CSPOMConstraint
import cspom.compiler.ConstraintCompiler
import cspom.variable.CSPOMConstant
import cspom.variable.SimpleExpression

/**
 * Unary disjunction transformed to equality
 */
object UnaryOr extends ConstraintCompiler {

  type A = (Boolean, SimpleExpression[Boolean])

  override def constraintMatcher = {
    case CSPOMConstraint(CSPOMConstant(res: Boolean), 'or, Seq(arg: SimpleExpression[Boolean]), params) =>
      (res, arg)
  }

  def compile(fc: CSPOMConstraint[_], problem: CSPOM, data: A) = {

    val (res, arg) = data

    val Seq(constant: Boolean) = fc.params.getOrElse("revsign", Seq(false))

    replaceCtr(fc, CSPOMConstraint('eq, Seq(arg, CSPOMConstant(constant ^ res))), problem)

  }

  def selfPropagation = false
}
