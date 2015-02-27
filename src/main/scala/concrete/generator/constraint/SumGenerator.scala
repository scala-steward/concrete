package concrete.generator.constraint;

import Generator.cspom2concrete
import concrete.constraint.semantic.Sum
import concrete.constraint.semantic.SumMode
import cspom.CSPOMConstraint
import cspom.variable.CSPOMConstant
import cspom.variable.CSPOMSeq
import concrete.constraint.semantic.SumAC
import concrete.constraint.semantic.SumBC
import SumMode._

final object SumGenerator extends Generator {

  override def gen(constraint: CSPOMConstraint[Boolean])(implicit variables: VarMap) = {
    val Seq(CSPOMSeq(vars), CSPOMConstant(c)) = constraint.arguments //map cspom2concreteVar

    // For bool2int optimization
    val const = c match {
      case i: Int => i
      case false  => 0
      case true   => 1
    }

    val params = constraint.params.get("coefficients") match {
      case Some(p: Seq[_]) => p.asInstanceOf[Seq[Int]]
      case None            => Seq.fill(vars.length)(1)
      case _               => throw new IllegalArgumentException("Parameters for zero sum must be a sequence of integer values")
    }

    val (solverVariables, varParams) = (vars map cspom2concrete)
      .zip(params)
      .collect {
        case (Var(v), p) => (v, p)
      }
      .unzip

    val constant = const - (vars map cspom2concrete)
      .zip(params)
      .collect {
        case (Const(c), p) => c * p
      }
      .sum

    val mode = constraint.params.get("mode").collect {
      case m: String => SumMode.withName(m)
    }.get

    if (solverVariables.isEmpty) {
      mode match {
        case SumEQ => require(constant == 0)
        case SumLT => require(constant > 0)
        case SumLE => require(constant >= 0, s"inconsistent sum $constraint")
        case SumNE => require(constant != 0)
      }
      Seq()
    } else {

      val bc = new SumBC(constant, varParams.toArray, solverVariables.toArray, mode)

      if (CSPOMSeq(constraint.arguments: _*).searchSpace < 1000) {
        Seq(bc, new SumAC(constant, varParams.toArray, solverVariables.toArray, mode))
      } else {
        Seq(bc)
      }
    }
    //    
    //    undefinedVar(solverVariables: _*) match {
    //      case Seq() => go(constant, varParams, solverVariables, mode)
    //      case Seq(uv) if mode == FilterSum.SumEQ =>
    //        val min = (solverVariables zip varParams).map { case (v, p) => if (v eq uv) 0 else v.dom.headValue * p }.sum
    //        val max = (solverVariables zip varParams).map { case (v, p) => if (v eq uv) 0 else v.dom.lastValue * p }.sum
    //        val factor = varParams(solverVariables.indexOf(uv))
    //        Generator.restrictDomain(uv, ((Interval(min, max) - constant) / -factor).range.iterator)
    //        go(constant, varParams, solverVariables, mode)
    //      case _ => None
    //    }

  }
}
