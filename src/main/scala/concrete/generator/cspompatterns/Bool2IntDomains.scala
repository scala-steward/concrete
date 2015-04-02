package concrete.generator.cspompatterns

import cspom.CSPOMConstraint
import cspom.VariableNames
import cspom.compiler.VariableCompiler
import cspom.util.IntInterval._
import cspom.util.IntInterval
import cspom.util.IntervalsArithmetic._
import cspom.util.RangeSet
import cspom.variable.BoolVariable
import cspom.variable.CSPOMConstant
import cspom.variable.CSPOMExpression
import cspom.variable.IntVariable
import cspom.variable.IntExpression.implicits.arithmetics
import cspom.variable.IntExpression
import cspom.variable.IntExpression.implicits.ranges
import cspom.variable.SimpleExpression
import cspom.compiler.ConstraintCompilerNoData
import cspom.CSPOM
import cspom.util.Finite
import cspom.compiler.Delta
import cspom.variable.BoolExpression

object Bool2IntDomains extends VariableCompiler('bool2int) {

  def compiler(c: CSPOMConstraint[_]) = c match {
    case CSPOMConstraint(CSPOMConstant(true), _, Seq(i0: SimpleExpression[_], i1: SimpleExpression[_]), params) =>

      val b = BoolExpression(i0)

      val ii = BoolExpression.span(b)

      val iii = reduceDomain(IntExpression(i1), ii)

      val bb =
        if (iii.contains(0)) {
          require(b.contains(false))
          if (iii.contains(1)) {
            require(b.contains(true))
            b
          } else {
            CSPOMConstant(false)
          }
        } else if (iii.contains(1)) {
          require(b.contains(true))
          CSPOMConstant(true)
        } else {
          throw new AssertionError()
        }

      Map(i0 -> bb, i1 -> iii)

  }
}

object Bool2IntIsEq extends ConstraintCompilerNoData {
  def matchBool(c: CSPOMConstraint[_], p: CSPOM) = c.function == 'bool2int
  def compile(c: CSPOMConstraint[_], p: CSPOM) = {
    val Seq(b, i) = c.arguments.map(_.asInstanceOf[SimpleExpression[_]])
    //(0).asInstanceOf[SimpleExpression[_]]
    // val i = c.arguments(1).asInstanceOf[SimpleExpression[_]]
    require(b.searchSpace == i.searchSpace &&
      (!b.contains(false) || i.contains(0) || i.contains(false)) && (
        !b.contains(true) || i.contains(1) || i.contains(true)))
    //println(s"removing $c")
    removeCtr(c, p) ++ replace(i, b, p)

  }
  def selfPropagation = false
}