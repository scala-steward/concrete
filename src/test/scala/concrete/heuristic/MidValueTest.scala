package concrete.heuristic

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import concrete.IntDomain
import concrete.Variable


/**
 * @author vion
 */
class MidValueTest extends FlatSpec with Matchers {
  "MidValue" should "select value closest to mean" in {
    val i = IntDomain.ofInterval(0, 1)
    val j = IntDomain.ofSeq(9998, 10000)
    val d = i | j

    d.view.toSeq should contain theSameElementsInOrderAs Seq(0, 1, 9998, 10000)

    val v = new Variable("v", d)
    val h = new concrete.heuristic.value.MidValue()

    h.selectIndex(v, d) shouldBe 9998
  }

}