package concrete.heuristic

import concrete.{IntDomain, Problem, Variable}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


/**
 * @author vion
 */
class MidValueTest extends AnyFlatSpec with Matchers {
  "MidValue" should "select value closest to mean" in {
    val i = IntDomain.ofInterval(0, 1)
    val j = IntDomain.ofSeq(9998, 10000)
    val d = i | j

    d should contain theSameElementsInOrderAs Seq(0, 1, 9998, 10000)

    val v = new Variable("v", d)
    val h = new concrete.heuristic.value.MidValue()
    val ps = Problem(v).initState.toState

    h.select(ps, v, d)._2 should contain theSameElementsAs Seq(9998)
  }

}