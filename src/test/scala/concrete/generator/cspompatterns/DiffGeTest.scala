package concrete.generator.cspompatterns

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import concrete.CSPOMDriver.CSPOMIntExpressionOperations
import cspom.CSPOM
import cspom.CSPOM.ctr
import cspom.compiler.CSPOMCompiler
import cspom.compiler.MergeEq
import cspom.variable.BoolVariable
import cspom.variable.CSPOMConstant
import cspom.variable.IntVariable

class DiffGeTest extends FlatSpec with Matchers {

  "DiffGe" should "compile general" in {

    val cspom = CSPOM { implicit problem =>

      val r = IntVariable(1 to 3) - IntVariable(2 to 4)

      ctr(r >= IntVariable(0 to 5))

    }

    CSPOMCompiler.compile(cspom, Seq(MergeEq, DiffGe))

    cspom.referencedExpressions should have size 4
    cspom.constraints should have size 1

    val c = cspom.constraints.next

    c.function shouldEqual 'diffGe

    c.result match {
      case CSPOMConstant(true) =>
      case _                   => fail()
    }

  }

  it should "compile functional" in {
    val cspom = CSPOM { implicit problem =>

      val r = IntVariable(1 to 3) - IntVariable(2 to 4)

      val r2 = (r >= IntVariable(0 to 5))

    }

    CSPOMCompiler.compile(cspom, Seq(DiffGe))

    cspom.referencedExpressions should have size 4
    cspom.constraints should have size 1
    val c = cspom.constraints.next
    c.function shouldEqual 'diffGe
    c.result shouldBe a[BoolVariable]
  }

}
