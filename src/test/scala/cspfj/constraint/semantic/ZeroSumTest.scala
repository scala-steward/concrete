package cspfj.constraint.semantic;

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import cspfj.constraint.Constraint
import cspfj.problem.BitVectorDomain
import cspfj.problem.Variable

final class ZeroSumTest {

  private var v0: Variable = null;
  private var v1: Variable = null;
  private var c: Constraint = null;

  @Before
  def setUp() {
    v0 = new Variable("v0", new BitVectorDomain(1, 2, 3, 4));
    v1 = new Variable("v1", new BitVectorDomain(0, 1, 2, 3, 4));
    val b = new Variable("b", new BitVectorDomain(1))
    c = new ZeroSum(Array(4, -1, -1), Array(b, v0, v1));
  }

  @Test
  def reviseTest() {
    (0 until c.arity).foreach(c.setRemovals)
    c.revise()

    assertEquals(Seq(1, 2, 3, 4), v0.dom.values.toSeq);
    assertEquals(Seq(0, 1, 2, 3), v1.dom.values.toSeq);
  }

}