package concrete.generator.cspompattern

import org.junit.Assert.assertEquals
import org.junit.Test
import cspom.CSPOM
import cspom.CSPOM._
import concrete.generator.cspompatterns.AllDiff
import concrete.CSPOMDriver._
import cspom.compiler.MergeEq
import cspom.compiler.ProblemCompiler
import cspom.variable.IntVariable

class AllDiffTest {
  @Test
  def testExt() {
    val cspom = CSPOM { implicit problem =>

      val v0 = IntVariable(Seq(1, 2, 3))
      val v1 = IntVariable(Seq(2, 3, 4))
      val v2 = IntVariable(Seq(1, 2, 3))
      val v3 = IntVariable(Seq(1, 2, 3))

      for (Seq(p1, p2) <- List(v0, v1, v2, v3).combinations(2)) {
        ctr(p1 ≠ p2)
      }
    }

    ProblemCompiler.compile(cspom, Seq(MergeEq, AllDiff))

    //println(cspom)
    assertEquals(1, cspom.constraints.size)
    assertEquals('allDifferent, cspom.constraints.next.function)
  }

}