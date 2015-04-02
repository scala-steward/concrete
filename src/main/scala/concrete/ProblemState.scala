package concrete

import concrete.constraint.Constraint
import concrete.constraint.StatefulConstraint
import concrete.util.BitVector
import concrete.util.Interval
import scala.reflect.runtime.universe._
import cspom.UNSATException

sealed trait Outcome {
  def andThen(f: ProblemState => Outcome): Outcome
  def filterDom(id: Int)(f: Int => Boolean): Outcome
  def filterDom(v: Variable)(f: Int => Boolean): Outcome

  def shaveDom(id: Int, lb: Int, ub: Int): Outcome
  def shaveDom(id: Int, itv: Interval): Outcome = shaveDom(id, itv.lb, itv.ub)

  def shaveDom(v: Variable, lb: Int, ub: Int): Outcome
  def shaveDom(v: Variable, itv: Interval): Outcome = shaveDom(v, itv.lb, itv.ub)

  def removeTo(v: Variable, ub: Int): Outcome = removeTo(v.id, ub)
  def removeFrom(v: Variable, lb: Int): Outcome = removeFrom(v.id, lb)
  def removeUntil(v: Variable, ub: Int): Outcome = removeUntil(v.id, ub)
  def removeAfter(v: Variable, lb: Int): Outcome = removeAfter(v.id, lb)

  def removeTo(id: Int, ub: Int): Outcome
  def removeFrom(id: Int, lb: Int): Outcome
  def removeUntil(id: Int, ub: Int): Outcome
  def removeAfter(id: Int, lb: Int): Outcome

  def entail(id: Int): Outcome
  def entail(c: Constraint): Outcome = entail(c.id)

  def entailIfFree(c: Constraint): Outcome

  def updateDom(v: Variable, d: Domain): Outcome
  def updateDom(id: Int, d: Domain): Outcome

  def updateAll(vars: Iterable[Variable])(f: Domain => Domain): Outcome = updateAll(vars.iterator)(f)
  def updateAll(vars: Iterator[Variable])(f: Domain => Domain): Outcome = {
    var ch = this
    while (vars.hasNext) {
      if (ch eq Contradiction) return Contradiction
      var v = vars.next
      ch = ch.updateDom(v, f(dom(v)))
    }
    ch
  }

  def updateDomains(v: Array[Variable], newDomains: IndexedSeq[Domain]): Outcome = {
    var i = v.length - 1
    var ps = this
    while (i >= 0) {
      ps = ps.updateDom(v(i), newDomains(i))
      i -= 1
    }
    ps
  }

  def assign(p: Pair): Outcome = assign(p.variable, p.value)
  def remove(p: Pair): Outcome = remove(p.variable, p.value)

  def assign(v: Variable, value: Int): Outcome = assign(v.id, value)
  def remove(v: Variable, value: Int): Outcome = remove(v.id, value)

  def assign(id: Int, value: Int): Outcome
  def remove(id: Int, value: Int): Outcome

  def dom(id: Int): Domain
  def dom(v: Variable): Domain

  def span(id: Int): Interval = dom(id).span
  def span(v: Variable): Interval = dom(v).span

  def boolDom(id: Int): BooleanDomain
  def boolDom(v: Variable): BooleanDomain = boolDom(v.id)

  def domains(v: Iterable[Variable]): Iterator[Domain] = {
    v.iterator.map(dom)
  }

  def updateState(id: Int, newState: AnyRef): Outcome

  def updateState(c: Constraint, newState: AnyRef): Outcome =
    updateState(c.id, newState)

  def domainsOption: Option[IndexedSeq[Domain]]
}

case object Contradiction extends Outcome {
  def andThen(f: ProblemState => Outcome) = Contradiction
  def filterDom(id: Int)(f: Int => Boolean): Outcome = Contradiction
  def filterDom(v: Variable)(f: Int => Boolean): Outcome = Contradiction
  def shaveDom(id: Int, lb: Int, ub: Int): Outcome = Contradiction
  def shaveDom(v: Variable, lb: Int, ub: Int): Outcome = Contradiction
  def entailIfFree(c: Constraint): Outcome = Contradiction
  def entail(id: Int) = Contradiction
  def removeTo(id: Int, ub: Int): Outcome = Contradiction
  def removeFrom(id: Int, lb: Int): Outcome = Contradiction
  def removeUntil(id: Int, ub: Int): Outcome = Contradiction
  def removeAfter(id: Int, ub: Int): Outcome = Contradiction
  def updateDom(id: Int, d: Domain): Outcome = Contradiction
  def updateDom(v: Variable, d: Domain): Outcome = Contradiction
  def assign(id: Int, value: Int): Outcome = Contradiction
  def remove(id: Int, value: Int): Outcome = Contradiction
  def dom(id: Int): Domain = throw new UNSATException("Tried to get a domain from a Contradiction")
  def dom(v: Variable): Domain = throw new UNSATException("Tried to get a domain from a Contradiction")
  def boolDom(id: Int): BooleanDomain = EMPTY

  def updateState(id: Int, newState: AnyRef): Outcome = Contradiction
  def domainsOption(): Option[IndexedSeq[Domain]] = None

}

final case class ProblemState(
  val domains: IndexedSeq[Domain],
  val constraintStates: IndexedSeq[AnyRef],
  val entailed: BitVector) extends Outcome {

  def andThen(f: ProblemState => Outcome) = f(this)

  def apply[A: TypeTag](c: Constraint): A = constraintStates(c.id).asInstanceOf[A]

  def updateState(id: Int, newState: AnyRef): ProblemState = {
    if (constraintStates(id) eq newState) {
      this
    } else {
      new ProblemState(domains, constraintStates.updated(id, newState), entailed)
    }
  }

  //  def updateConstraints(f: (Int, Any) => Any) = {
  //    var i = constraintStates.length - 1
  //    val builder = constraintStates.genericBuilder[Any]
  //    builder.sizeHint(constraintStates.length)
  //    while (i < constraintStates.length) {
  //      builder += f(i, constraintStates(i))
  //    }
  //    new ProblemState(domains, builder.result, entailed)
  //  }

  def padConstraints(constraints: IndexedSeq[Constraint]) = {
    val padded: IndexedSeq[AnyRef] = constraintStates ++: constraints.drop(constraintStates.size).map {
      case c: StatefulConstraint => c.initState
      case _                     => null
    }
    if (padded eq constraintStates) this else new ProblemState(domains, padded, entailed)
  }

  def entail(id: Int): ProblemState = new ProblemState(domains, constraintStates, entailed + id)

  def isEntailed(c: Constraint): Boolean = entailed(c.id)

  def updateDom(id: Int, newDomain: Domain): Outcome =
    if (newDomain.isEmpty) {
      Contradiction
    } else {
      updateDomNonEmpty(id, newDomain)
    }

  def updateDom(v: Variable, newDomain: Domain): Outcome = {
    if (newDomain.isEmpty) {
      Contradiction
    } else {
      updateDomNonEmpty(v, newDomain)
    }
  }

  private def updateDomNonEmpty(id: Int, oldDomain: Domain, newDomain: Domain): ProblemState = {
    assert(newDomain.nonEmpty)
    if (oldDomain eq newDomain) {
      this
    } else {
      assert(newDomain subsetOf oldDomain)
      new ProblemState(domains.updated(id, newDomain), constraintStates, entailed)
    }
  }

  def updateDomNonEmpty(id: Int, newDomain: Domain): ProblemState = {
    updateDomNonEmpty(id, dom(id), newDomain)
  }

  def updateDomNonEmpty(variable: Variable, newDomain: Domain): ProblemState = {
    updateDomNonEmpty(variable.id, dom(variable), newDomain)
  }

  def shaveDomNonEmpty(variable: Variable, itv: Interval): ProblemState = {
    updateDomNonEmpty(variable, dom(variable) & itv)
  }

  def dom(id: Int): Domain = domains(id)
  def dom(v: Variable): Domain = {
    val id = v.id
    if (id < 0) v.initDomain else domains(id)
  }

  def boolDom(id: Int): BooleanDomain = domains(id).asInstanceOf[BooleanDomain]

  def assign(id: Int, value: Int): Outcome = {
    updateDom(id, domains(id).assign(value))
  }

  def remove(id: Int, value: Int): Outcome = {
    updateDom(id, domains(id).remove(value))
  }

  def filterDom(id: Int)(f: Int => Boolean) =
    updateDom(id, domains(id).filter(f))

  def filterDom(v: Variable)(f: Int => Boolean) =
    updateDom(v, dom(v).filter(f))

  def shaveDom(id: Int, lb: Int, ub: Int): Outcome =
    updateDom(id, domains(id) & (lb, ub))

  def shaveDom(v: Variable, lb: Int, ub: Int): Outcome =
    updateDom(v, dom(v) & (lb, ub))

  def removeTo(id: Int, ub: Int): Outcome =
    updateDom(id, domains(id).removeTo(ub))

  def removeFrom(id: Int, lb: Int): Outcome =
    updateDom(id, domains(id).removeFrom(lb))
  def removeUntil(id: Int, ub: Int): Outcome =
    updateDom(id, domains(id).removeUntil(ub))
  def removeAfter(id: Int, lb: Int): Outcome =
    updateDom(id, domains(id).removeAfter(lb))

  def entailIfFree(c: Constraint) = {
    if (c.isFree(this)) entail(c.id) else this
  }

  def domainsOption = Some(domains)

}