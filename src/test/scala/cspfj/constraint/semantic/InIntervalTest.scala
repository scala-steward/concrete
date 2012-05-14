package cspfj.constraint.semantic;

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import cspfj.IntDomain
import cspfj.Domain
import cspfj.Variable

final class InIntervalTest {

  private var domain: Domain = null;
  private var constraint: InInterval = null;

  @Before
  def setUp() {
    domain = IntDomain(1 to 3)
    val variable = new Variable("test", domain);
    constraint = InInterval.values(variable, 2, 4);
  }

  @Test
  def testIsConsistent() {
    assertTrue(constraint.isConsistent());
  }

  @Test
  def testRevise() {
    constraint.revise()
    assertEquals(Seq(2, 3), domain.values.toSeq);
  }

}
