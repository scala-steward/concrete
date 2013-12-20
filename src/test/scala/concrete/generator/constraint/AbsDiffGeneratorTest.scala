package concrete.generator.constraint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import concrete.constraint.semantic.AbsDiff
import concrete.generator.ProblemGenerator
import concrete.Problem
import cspom.CSPOM
import cspom.CSPOMConstraint
import cspom.CSPOM._

final class AbsDiffGeneratorTest {

  @Test
  def test() {

    val (cspom, c) = CSPOM withResult {
      val v0 = varOf(1, 2, 3)
      val v1 = varOf(1, 2, 3)
      val v2 = varOf(1, 2, 3)
      ctr(new CSPOMConstraint(v0, 'absdiff, v1, v2))
    }

    val problem = new Problem(ProblemGenerator.generateVariables(cspom))
    AbsDiffGenerator.generate(c, problem)

    assertEquals(1, problem.constraints.size)
    assertTrue(problem.constraints.head.isInstanceOf[AbsDiff])

  }
}