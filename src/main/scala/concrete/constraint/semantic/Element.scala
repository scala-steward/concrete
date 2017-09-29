package concrete
package constraint
package semantic

import bitvectors.BitVector

object Element {
  def apply(result: Variable, index: Variable, varsIdx: Seq[(Int, Variable)]) = {
    val lastIndex = varsIdx.map(_._1).max
    val vars = Array.ofDim[Variable](lastIndex + 1)

    for ((i, v) <- varsIdx) {
      vars(i) = v
    }

    if (result eq index) {
      Seq(new ElementRI(result, vars))
    } else if (vars.forall(v => (v eq null) || v.initDomain.isAssigned)) {
      val values = vars.map(Option(_).map(_.initDomain.singleValue))
      Seq(new ElementVal(result, index, values))
    } else {
      Seq(new ElementWatch(result, index, vars))
    }
  }
}

class ElementVal(val result: Variable, val index: Variable, val valuesOpt: Array[Option[Int]]) extends Constraint(Array(result, index)) {

  var values: Array[Int] = _

  def advise(ps: ProblemState, event: Event, position: Int): Int = ps.card(result) + ps.card(index)

  def check(tuple: Array[Int]): Boolean = {
    valuesOpt(tuple(1)).contains(tuple(0))
  }

  def init(ps: ProblemState): Outcome = {
    values = valuesOpt.map {
      case Some(v) => v
      case None => Int.MinValue
    }

    ps.filterDom(index)(valuesOpt(_).isDefined)
  }

  def revise(ps: ProblemState): Outcome = {
    val res = ps.dom(result)

    ps.filterDom(index)(i => res.present(values(i)))
      .andThen { ps =>
        var bv: IntDomain = EmptyIntDomain
        for (i <- ps.dom(index)) {
          bv |= values(i)
        }
        ps.intersectDom(result, bv)
      }
      .entailIf(this, _.dom(result).isAssigned)
  }

  def simpleEvaluation: Int = ???
}

class ElementRI(val resultIndex: Variable, val vars: Array[Variable]) extends Constraint(resultIndex +: vars.flatMap(Option(_))) {
  lazy val vars2pos = {
    val scopeIndices = scope.zipWithIndex.drop(1).toMap
    Array.tabulate(vars.length)(i => Option(vars(i)).map(scopeIndices).getOrElse(-1))
  }

  def advise(problemState: ProblemState, event: Event, position: Int): Int = arity

  def check(tuple: Array[Int]): Boolean = {
    tuple(0) < vars.length &&
      (vars(tuple(0)) ne null) &&
      (tuple(0) == tuple(vars2pos(tuple(0))))
  }

  def init(ps: ProblemState): Outcome = ps

  def revise(ps: ProblemState): Outcome = {
    ps.filterDom(resultIndex) { i =>
      ps.dom(vars(i)).present(i)
    }.andThen { ps =>
      if (ps.dom(resultIndex).isAssigned) {
        val value = ps.dom(resultIndex).singleValue
        ps.tryAssign(vars(value), value)
      } else {
        ps
      }
    }
  }

  def simpleEvaluation: Int = 2
}

class ElementWatch(val result: Variable,
                   val index: Variable,
                   val vars: Array[Variable])
  extends Constraint(result +: index +: vars.flatMap(Option(_))) with Removals {

  require(result ne index)

  lazy val vars2pos = {
    val scopeIndices = scope.zipWithIndex.drop(2).toMap
    Array.tabulate(vars.length)(i => Option(vars(i)).map(scopeIndices).getOrElse(-1))
  }

  val pos2vars = Array.fill(arity)(-1) //new Array[Int](arity)

  fillPos2Vars(2, 0)
  /**
    * For each value v in result, contains an index i for which vars(i) may contain v
    */
  private[semantic] val resultWatches = new java.util.HashMap[Int, Int]
  /**
    * For each index i, contains a value "v" for which result and vars(i) may contain v
    */
  private val indexWatches = new Array[Int](vars.length)
  /**
    * For each variable in vars, counts the number of current resultWatches
    */
  private[semantic] val watched = Array.fill(vars.length)(0)
  private var card: Int = _

  def check(tuple: Array[Int]): Boolean = {
    tuple(1) < vars.length &&
      (vars(tuple(1)) ne null) &&
      (tuple(0) == tuple(vars2pos(tuple(1))))
  }

  def getEvaluation(ps: ProblemState): Int = {
    card * ps.card(index)
  }

  override def init(ps: ProblemState) = {
    val notnull = ps.dom(index).filter(i => i >= 0 && i < vars.length && (vars(i) ne null))
    card = notnull.view.map(i => ps.card(vars(i))).max
    ps.updateDom(index, notnull)
  }

  //  def toString(ps: ProblemState, consistency: String): String = {
  //    s"${ps.dom(result)} =$consistency= ${ps.dom(index)}th of ${
  //      vars.toSeq.map {
  //        case null => "{}"
  //        case v => ps.dom(v)
  //      }
  //    }"
  //  }

  def revise(ps: ProblemState, mod: BitVector): Outcome = {

    //    println(s"revising ${toString(ps)}")
    //    println(s"constraint $id has mod $mod, watches $resultWatches and [${watched.mkString(", ")}]")

    val m = mod.nextSetBit(0)
    assert(m >= 0)
    var rResult = false
    var rIndex = false

    val resultDom = ps.dom(result)

    if (m >= 2) {
      // result and index are not modified
      rResult = invalidResultWatch(ps, mod)
      rIndex = invalidIndexWatch(ps, mod, resultDom)
    } else if (m == 1) {
      // index is modified, result is not modified
      rResult = true
      rIndex = invalidIndexWatch(ps, mod, resultDom)
    } else {
      // result is modified
      rIndex = true
      rResult = (mod.nextSetBit(1) == 1) || invalidResultWatch(ps, mod)
    }

    (if (rIndex) {
      reviseIndex(ps, resultDom)
    } else {
      ps
    }).andThen { ps =>

      val index = ps.dom(this.index)

      if (index.isAssigned) {
        reviseAssignedIndex(ps, index.singleValue, resultDom)
      } else if (rResult) {
        reviseResult(ps, index)
      } else {
        ps
      }
    }
  }

  private def reviseIndex(ps: ProblemState, resultDom: Domain): Outcome = {
    ps.filterDom(this.index) { i =>
      val dom = ps.dom(vars(i))
      val v = indexWatches(i)
      (resultDom.present(v) && dom.present(v)) || {
        val support = (resultDom & dom).headOption
        support.foreach(s => indexWatches(i) = s)
        support.isDefined
      }
    }
  }

  private def reviseAssignedIndex(ps: ProblemState, index: Int, resultDom: Domain): Outcome = {
    val selectedVar = vars(index)
    //println(selectedVar.toString(ps))
    val intersect = ps.dom(selectedVar) & resultDom

    // println(s"${ps.dom(selectedVar)} & $resultDom = $intersect")

    ps
      .updateDom(selectedVar, intersect)
      .updateDom(result, intersect)
  }

  private def reviseResult(ps: ProblemState, index: Domain): Outcome = {
    ps.filterDom(result) { i =>
      val oldWatch: Integer = resultWatches.get(i)

      if (oldWatch != null && index.present(oldWatch) && ps.dom(vars(oldWatch)).present(i)) {
        true
      } else {
        val support = index.find(j => ps.dom(vars(j)).present(i))
        support.foreach { s =>
          if (oldWatch != null) {
            watched(oldWatch) -= 1
          }
          resultWatches.put(i, s)
          watched(s) += 1
        }
        support.isDefined
      }

    }
  }

  private def invalidResultWatch(ps: ProblemState, mod: BitVector): Boolean = {
    var m = mod.nextSetBit(2)
    while (m >= 0) {
      val i = pos2vars(m)
      if (watched(i) > 0) {
        return true
      }
      m = mod.nextSetBit(m + 1)
    }
    false
  }

  private def invalidIndexWatch(ps: ProblemState, mod: BitVector, resultDom: Domain): Boolean = {
    var m = mod.nextSetBit(2)
    while (m >= 0) {
      val i = pos2vars(m)
      if (!resultDom.present(indexWatches(i)) || !ps.dom(vars(i)).present(indexWatches(i))) {
        return true
      }
      m = mod.nextSetBit(m + 1)
    }
    false
  }

  def simpleEvaluation: Int = 3

  override def toString(ps: ProblemState) = toString(ps, "AC")

  def toString(ps: ProblemState, consistency: String): String = {
    s"${result.toString(ps)} =$consistency= ${index.toString(ps)}th of [${
      vars.map(Option(_).map(_.toString(ps)).getOrElse("{}")).mkString(", ")
    }]"
  }

  private def fillPos2Vars(scopeI: Int, varsI: Int): Unit = {
    if (scopeI < arity) {
      Option(vars(varsI)) match {
        case None => fillPos2Vars(scopeI, varsI + 1)
        case Some(v) =>
          assert(scope(scopeI) == v)
          pos2vars(scopeI) = varsI
          fillPos2Vars(scopeI + 1, varsI + 1)
      }
    }
  }
}
