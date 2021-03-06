package concrete.constraint.extension

;

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import concrete.IntDomain
import concrete.Problem
import concrete.Variable
import concrete.constraint.AdviseCount
import mdd.MDD

final class ExtensionConstraintTrieTest extends FlatSpec with Matchers {

  val mdd = MDD(Array(0, 0), Array(1, 1), Array(2, 2))

  // val ta = new MDDMatrix(mdd, false)

  val v0 = new Variable("V0", IntDomain(0 to 1))
  val v1 = new Variable("V1", IntDomain(0 to 2))

  val mmd = new ReduceableExt(Array(v0, v1), new MDDRelation(mdd))
  mmd.register(new AdviseCount())

  //println(content map (_.toSeq) mkString (", "))

  "ReduceableExt" should "filter" in {
    val problem = Problem(v0, v1)
    problem.addConstraint(mmd)
    val state = problem.initState.toState
    mmd.eventAll(state)

    val mod = mmd.revise(state).toState

    mod.dom(v0) should be theSameInstanceAs v0.initDomain
    mod.dom(v1) should not be theSameInstanceAs(v1.initDomain)
    mod(mmd) should have size 2

  }
}

