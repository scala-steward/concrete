package concrete

import bitvectors.BitVector
import com.typesafe.scalalogging.LazyLogging
import concrete.constraint.{Constraint, StatefulConstraint}
import concrete.util.{IdentityMap, Interval}
import cspom.UNSATException

import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.immutable.IntMap

sealed trait Outcome {
  def tryAssign(variable: Variable, i: Int): Outcome

  def andThen(f: ProblemState => Outcome): Outcome

  def orElse[A >: ProblemState](f: => A): A

  def map[A](f: ProblemState => A): Option[A]

  def filterDom(v: Variable)(f: Int => Boolean): Outcome

  def filterBounds(v: Variable)(f: Int => Boolean): Outcome

  def shaveDom(v: Variable, lb: Int, ub: Int): Outcome

  def shaveDom(v: Variable, itv: Interval): Outcome = shaveDom(v, itv.lb, itv.ub)

  def intersectDom(v: Variable, d: Domain): Outcome = {
    updateDom(v, dom(v) & d)
  }

  def removeTo(v: Variable, ub: Int): Outcome

  def removeFrom(v: Variable, lb: Int): Outcome

  def removeUntil(v: Variable, ub: Int): Outcome

  def removeAfter(v: Variable, lb: Int): Outcome

  def entail(c: Constraint): Outcome

  def entail(c: Constraint, i: Int): Outcome

  def entailIfFree(c: Constraint): Outcome

  def entailIfFree(c: Constraint, doms: Array[Domain]): Outcome

  def entailIf(c: Constraint, f: ProblemState => Boolean): Outcome

  def updateDom(v: Variable, d: Domain): Outcome

  def assign(v: Variable, value: Int): Outcome

  def remove(v: Variable, value: Int): Outcome

  def doms(vs: Array[Variable]): Array[Domain] = {
    val d = new Array[Domain](vs.length)
    var i = vs.length - 1
    while (i >= 0) {
      d(i) = dom(vs(i))
      i -= 1
    }
    d
  }

  def dom(v: Variable): Domain

  def card(v: Variable): Int = dom(v).size

  def span(v: Variable): Interval = dom(v).span

  //def boolDom(v: Variable): BooleanDomain = dom(v).asInstanceOf[BooleanDomain]

  def updateState[S <: AnyRef](c: StatefulConstraint[S], newState: S): Outcome

  //def domainsOption: Option[IndexedSeq[Domain]]

  //def toString(problem: Problem): String
  def toState: ProblemState

  //def isEntailed(c: Constraint): Boolean
  def activeConstraints(v: Variable): BitVector

  def apply[S <: AnyRef](c: StatefulConstraint[S]): S

  def isState: Boolean

  def fold[A](s: Iterable[A])(f: (ProblemState, A) => Outcome): Outcome = {
    fold(s.iterator, f)
  }

  @tailrec
  final def fold[A](it: Iterator[A], f: (ProblemState, A) => Outcome): Outcome = {
    this match {
      case ps: ProblemState if it.hasNext => f(ps, it.next()).fold(it, f)
      case e => e
    }
  }

  def dueTo(cause: => (Constraint, Iterable[Variable])): Outcome
}

object Contradiction {
  def apply(to: Variable): Contradiction = Contradiction(Seq(to))

  def apply(to: Array[Variable]): Contradiction = Contradiction(immutable.ArraySeq.unsafeWrapArray(to))

  def apply(to: Seq[Variable]): Contradiction = Contradiction(None, Seq.empty, to)
}

case class Contradiction(cause: Option[Constraint], from: Seq[Variable], to: Seq[Variable]) extends Outcome {
  def andThen(f: ProblemState => Outcome): Outcome = this

  def orElse[A >: ProblemState](f: => A): A = f

  def map[A](f: ProblemState => A): Option[A] = None

  def filterDom(v: Variable)(f: Int => Boolean): Outcome = this

  def filterBounds(v: Variable)(f: Int => Boolean): Outcome = this

  def shaveDom(v: Variable, lb: Int, ub: Int): Outcome = this

  def entailIfFree(c: Constraint): Outcome = this

  def entailIfFree(c: Constraint, doms: Array[Domain]): Outcome = this

  def entailIf(c: Constraint, f: ProblemState => Boolean): Outcome = this

  def removeTo(v: Variable, ub: Int): Outcome = this

  def removeFrom(v: Variable, lb: Int): Outcome = this

  def removeUntil(v: Variable, ub: Int): Outcome = this

  def removeAfter(v: Variable, lb: Int): Outcome = this

  def updateDom(v: Variable, d: Domain): Outcome = this

  def remove(v: Variable, value: Int): Outcome = this

  def dom(v: Variable): Domain = throw new UNSATException("Tried to get a domain from a Contradiction")

  def toState = throw new UNSATException("Tried to get state from a Contradiction")

  def apply[S <: AnyRef](c: StatefulConstraint[S]): S = throw new UNSATException("Tried to get state from a Contradiction")

  def assign(v: Variable, value: Int): concrete.Outcome = this

  def entail(c: Constraint): concrete.Outcome = this

  def entail(c: Constraint, i: Int): concrete.Outcome = this

  def activeConstraints(v: Variable): BitVector = throw new UNSATException("Tried to get state from a Contradiction")

  def updateState[S <: AnyRef](c: StatefulConstraint[S], newState: S): Outcome = this

  def isState = false

  def dueTo(cause: => (Constraint, Iterable[Variable])) = Contradiction(Some(cause._1), this.from ++ cause._2, to)

  override def tryAssign(variable: Variable, i: Int): Outcome = this

}

object ProblemState {


  def apply(problem: Problem, decisionVariables: Set[Variable]): Outcome = {
    val doms = problem.variables.zipWithIndex.map { case (x, i) => i -> x.initDomain }
      .to(IntMap)
    new ProblemState(
      domains = doms,
      entailed = EntailmentManager(problem.variables.toSeq)
    )
      .padConstraints(problem.constraints)
  }

  def isFree(doms: Array[Domain]): Boolean = {
    var one = false
    var i = doms.length - 1
    while (i >= 0) {
      if (!doms(i).isAssigned) {
        if (one) {
          return false
        }
        one = true
      }
      i -= 1
    }
    true
  }

  def singleFree(doms: Array[Domain]): Option[Int] = {
    var f = -1
    var i = doms.length - 1
    while (i >= 0) {
      if (!doms(i).isAssigned) {
        if (f >= 0) return None
        f = i
      }
      i -= 1
    }
    if (f < 0) None else Some(f)

  }

}

class ProblemState(
                    private val domains: IntMap[Domain],
                    private val constraintStates: Map[Int, AnyRef] = Map(),
                    private val initializedConstraints: Int = 0,
                    val entailed: EntailmentManager,
                    val recentUpdates: IntMap[Domain] = IntMap(),
                    //                    private val pending: IntMap[Domain] = IntMap(),
                    data: IdentityMap[AnyRef, Any] = new IdentityMap[AnyRef, Any]()) extends Outcome
  with LazyLogging {

  //  def this(domains: Vector[Domain], constraintStates: Vector[AnyRef], entailed: EntailmentManager)
  //  = this(domains, constraintStates, entailed, new IdentityMap[AnyRef, Any]())

  def clearRecent: ProblemState = {

    new ProblemState(domains, constraintStates, initializedConstraints, entailed, IntMap(), data)
  }

  def wDeg(v: Variable): Int = entailed.wDeg(v)

  def isState = true

  def andThen(f: ProblemState => Outcome): Outcome = f(this)

  def orElse[A >: ProblemState](f: => A): A = this

  def map[A](f: ProblemState => A) = Some(f(this))

  def apply[S <: AnyRef](c: StatefulConstraint[S]): S =
    constraintStates(c.id).asInstanceOf[S]

  def updateState[S <: AnyRef](c: StatefulConstraint[S], newState: S): ProblemState = {
    val id = c.id
    if (constraintStates.get(id).exists(_ eq newState)) {
      this
    } else {
      new ProblemState(domains, constraintStates.updated(id, newState), initializedConstraints, entailed, recentUpdates, data)
    }
  }


  def padConstraints(constraints: Array[Constraint]): Outcome = {
    val newConstraints = constraints.view.drop(initializedConstraints)

    new ProblemState(domains, constraintStates, constraints.length,
      entailed.addConstraints(newConstraints), recentUpdates, data)
      .fold(newConstraints)((p, c) => c.init(p))

  }

  //
  //
  //  def padConstraints(constraints: Array[Constraint]): Outcome = {
  //    assert(constraints.sliding(2).forall (array => array(0).id < array(1).id ))
  //
  //    new ProblemState(domains, constraintStates, lastId, entailed.addConstraints(newConstraints), recentUpdates, data)
  //
  //    constraints.reverseIterator.takeWhile(_.id > initializedConstraints)
  //
  //
  //    var i = constraints.length - 1
  //    var ps: Outcome = this
  //    while (constraints(i).id > initializedConstraints) {
  //      new ProblemState(domains, constraintStates, lastId, entailed.addConstraints(newConstraints), recentUpdates, data)
  //        .fold(newConstraints)((p, c) => if (!p.hasStateFor(c)) c.init(p))
  //    }
  //      //
  //      //      newConstraints.foldLeft(
  //      //        new ProblemState(domains, padded, entailed.addConstraints(newConstraints), pending, data): Outcome) {
  //      //        case (out, c) => out.andThen(c.init)
  //      //      }
  //
  //  }


  //def isEntailed(c: Constraint): Boolean = entailed(c)

  def activeConstraints(v: Variable): BitVector = entailed.active(v)

  def shaveDomNonEmpty(variable: Variable, itv: Interval): ProblemState = {
    updateDomNonEmpty(variable, dom(variable) & itv)
  }

  def assign(v: Variable, value: Int): ProblemState = {
    val assigned = dom(v).assign(value)
    assert(assigned.nonEmpty)
    updateDomNonEmpty(v, dom(v).assign(value))
  }

  //def isEntailed(c: Constraint): Boolean = entailed(c.id)

  def tryAssign(v: Variable, value: Int): Outcome = {
    val d = dom(v)
    if (d.contains(value)) {
      if (d.isAssigned) {
        this
      } else {
        updateDomNonEmptyNoCheck(v, d.assign(value))
      }
    } else {
      Contradiction(v)
    }

  }

  def remove(v: Variable, value: Int): Outcome = {
    updateDom(v, dom(v) - value)
  }

  def removeIfPresent(v: Variable, value: Int): Outcome = {
    val d = dom(v)
    if (d.contains(value)) {
      updateDom(v, d - value)
    } else {
      this
    }
  }

  def updateDom(v: Variable, newDomain: Domain): Outcome = {
    if (newDomain.isEmpty) {
      Contradiction(v)
    } else {
      updateDomNonEmpty(v, newDomain)
    }
  }

  def updateDomNonEmpty(variable: Variable, newDomain: Domain): ProblemState = {
    assert(newDomain.nonEmpty)
    val oldDomain = dom(variable)
    assert(newDomain subsetOf oldDomain, s"replacing $oldDomain with $newDomain: not a subset!")
    if (oldDomain.size == newDomain.size) {
      assert(oldDomain.subsetOf(newDomain), s"replacing $oldDomain with $newDomain: same size but not equal")
      this
    } else {
      assert(newDomain.size < oldDomain.size, s"replacing $oldDomain with $newDomain: domain size seems to have increased")
      // assert(!oldDomain.isInstanceOf[BooleanDomain] || newDomain.isInstanceOf[BooleanDomain], s"replaced $oldDomain with $newDomain: type changed")
      //assert(!oldDomain.isInstanceOf[IntDomain] || newDomain.isInstanceOf[IntDomain], s"replaced $oldDomain with $newDomain: type changed")
      updateDomNonEmptyNoCheck(variable, newDomain)
    }
  }

  def updateDomNonEmptyNoCheck(variable: Variable, newDomain: Domain): ProblemState = {
    assert(newDomain.nonEmpty)
    assert(dom(variable) ne newDomain)
    val id = variable.id
    assert(id >= 0 || (dom(variable) eq newDomain), s"$variable updated to $newDomain is not a problem variable")
    assert(!newDomain.isInstanceOf[BitVectorDomain] || (newDomain.size <= newDomain.last - newDomain.head))


    //    if (pending.size >= 64) {
    //      var sendDomains = domains.updated(id, newDomain)
    //      for ((id, d) <- pending) {
    //        sendDomains = sendDomains.updated(id, d)
    //      }
    //      new ProblemState(sendDomains, constraintStates, initializedConstraints, entailed, IntMap(), data)
    //    } else {
    //      new ProblemState(domains, constraintStates, initializedConstraints, entailed, pending.updated(id, newDomain), data)
    //    }

    new ProblemState(domains.updated(id, newDomain),
      constraintStates, initializedConstraints, entailed,
      recentUpdates.updated(id, newDomain),
      data)
  }

  def dom(v: Variable): Domain = {
    val id = v.id
    if (id < 0) v.initDomain else dom(id)
  }

  def filterDom(v: Variable)(f: Int => Boolean): Outcome =
    updateDom(v, dom(v).filter(f))

  def filterBounds(v: Variable)(f: Int => Boolean): Outcome =
    updateDom(v, dom(v).filterBounds(f))

  def shaveDom(v: Variable, lb: Int, ub: Int): Outcome =
    updateDom(v, dom(v) & (lb, ub))

  def removeTo(v: Variable, ub: Int): Outcome =
    updateDom(v, dom(v).removeTo(ub))

  override def removeFrom(v: Variable, lb: Int): Outcome =
    updateDom(v, dom(v).removeFrom(lb))

  override def removeUntil(v: Variable, ub: Int): Outcome =
    updateDom(v, dom(v).removeUntil(ub))

  override def removeAfter(v: Variable, lb: Int): Outcome =
    updateDom(v, dom(v).removeAfter(lb))

  def currentDomains: Iterator[Domain] = {
    Iterator.range(0, domains.size).map(i => dom(i))
  }

  def dom(i: Int): Domain = {
    domains(i) //.getOrElse(i, domains(i))
  }

  def entailIfFree(c: Constraint): ProblemState = c.singleFree(this).map(entail(c, _)).getOrElse(this)

  def entailIfFree(c: Constraint, doms: Array[Domain]): ProblemState = ProblemState.singleFree(doms).map(entail(c, _)).getOrElse(this)

  def entail(c: Constraint, i: Int): ProblemState = {
    new ProblemState(domains, constraintStates, initializedConstraints, entailed.entail(c, i), recentUpdates, data)
  }

  def entailIf(c: Constraint, f: ProblemState => Boolean): ProblemState = {
    if (f(this)) entail(c) else this
  }

  def entail(c: Constraint): ProblemState = {
    new ProblemState(domains, constraintStates, initializedConstraints, entailed.entail(c, this), recentUpdates, data)
    //else this
  }

  def toState: ProblemState = this

  def dueTo(cause: => (Constraint, Iterable[Variable])): ProblemState = this

  def updateData(key: AnyRef, value: Any) =
    new ProblemState(domains, constraintStates, initializedConstraints, entailed, recentUpdates, data.updated(key, value))

  def getData[A](key: AnyRef): A = data(key).asInstanceOf[A]

  def sameDomains(ps: ProblemState): Boolean = domains eq ps.domains

  override def toString = s"ProblemState($domains, $constraintStates, $entailed)"
}