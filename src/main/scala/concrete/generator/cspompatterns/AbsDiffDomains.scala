package concrete.generator.cspompatterns

import cspom.CSPOMConstraint
import cspom.compiler.VariableCompiler
import cspom.util.IntervalsArithmetic.RangeArithmetics
import cspom.variable.IntVariable.arithmetics
import cspom.variable.IntVariable.intExpression
import cspom.variable.IntVariable.ranges
import cspom.variable.SimpleExpression
import cspom.CSPOM

object AbsDiffDomains extends VariableCompiler('absdiff) {

  def compiler(c: CSPOMConstraint[_]) = c match {
    case CSPOMConstraint(ir: SimpleExpression[_], _, Seq(ii0: SimpleExpression[_], ii1: SimpleExpression[_]), _) =>
      val r = intExpression(ir)
      val i0 = intExpression(ii0)
      val i1 = intExpression(ii1)

      val nr = r & (i0 - i1).abs
      val ni0 = i0 & ((i1 + nr) ++ (i1 - nr))
      val ni1 = i1 & ((ni0 + nr) ++ (ni0 - nr))

      assert(nr == (nr & (ni0 - ni1).abs), s"$nr = |$ni0 - $ni1| still requires shaving (result is ${(ni0 - ni1).abs})")
      assert(ni0 == (ni0 & (ni1 + nr) ++ (ni1 - nr)), s"$ni0 still requires shaving")

      Map(
        ir -> reduceDomain(r, nr),
        ii0 -> reduceDomain(i0, ni0),
        ii1 -> reduceDomain(i1, ni1))

    case _ => throw new IllegalArgumentException
  }

  override def compile(c: CSPOMConstraint[_], problem: CSPOM, data: A) = {
    val e = problem.referencedExpressions
    val ct = e.count(_.fullyDefined)
    println(s"$ct/${e.size}")
    super.compile(c, problem, data)
  }
}