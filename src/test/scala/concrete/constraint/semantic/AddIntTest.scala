package concrete.constraint.semantic
import concrete.Problem
import org.junit.Test
import concrete.IntDomain
import org.junit.Assert._
import concrete.AdviseCount

final class AddIntTest {

  @Test
  def testX() {
    val prob = new Problem
    val x = prob.addVariable("x", IntDomain(-100 to 100))
    val y = prob.addVariable("y", IntDomain(5))
    val z = prob.addVariable("z", IntDomain(2))
    val c = new Add(x, y, z)
    prob.addConstraint(c)
    AdviseCount.adviseAll(c)
    assertTrue(c.isBound)
    assertEquals(Seq(0), c.revise())
    assertEquals(Seq(7), x.dom.values.toSeq)
    assertTrue(c.isBound)
  }

  @Test
  def testY() {
    val prob = new Problem
    val x = prob.addVariable("x", IntDomain(7))
    val y = prob.addVariable("y", IntDomain(-100 to 100))
    val z = prob.addVariable("z", IntDomain(2))
    val c = new Add(x, y, z)
    prob.addConstraint(c)
    AdviseCount.adviseAll(c)
    assertTrue(c.isBound)
    assertEquals(Seq(1), c.revise())
    assertEquals(Seq(5), y.dom.values.toSeq)
    assertTrue(c.isBound)
  }

  @Test
  def testZ() {
    val prob = new Problem
    val x = prob.addVariable("x", IntDomain(1 to 10))
    val y = prob.addVariable("y", IntDomain(20 to 30))
    val z = prob.addVariable("z", IntDomain(-100 to 100))
    val c = new Add(x, y, z)
    prob.addConstraint(c)
    AdviseCount.adviseAll(c)
    assertTrue(c.isBound)
    assertEquals(Seq(2), c.revise())
    assertEquals((-29 to -10).toSeq, z.dom.values.toSeq)
    assertTrue(c.isBound)
  }
}