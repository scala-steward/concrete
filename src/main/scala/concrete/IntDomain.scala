package concrete

import bitvectors.BitVector
import concrete.util.Interval
import cspom.util._

import scala.collection.immutable.TreeSet



object IntDomain {
  val DISPLAYED_VALUES = 5

  private val TREE_SET_THRESHOLD = 100

  def ofInterval(i: Interval): IntDomain = i.size match {
    case 0 => EmptyIntDomain
    case 1 => Singleton(i.lb)
    case _ => new IntervalDomain(i)
  }

  def apply(d: RangeSet[Infinitable]): IntDomain = {
    val hull = d.span

    if (d.isConvex) {
      ofInterval(Math.toIntExact(hull.lb.finite), Math.toIntExact(hull.ub.finite))
    } else {
      val asSet = new ContiguousIntRangeSet(d)
      val size = asSet.totalSize
      val hullSize = hull.itvSize.finite
      if ((asSet.totalSize * Finite(TREE_SET_THRESHOLD)) < hullSize) {
        val builder = TreeSet.newBuilder[Int]
        builder ++= asSet.iterator.map(Math.toIntExact)
        new TreeSetDomain(builder.result())
      } else {
        val bvb = new java.util.BitSet(Math.toIntExact(hullSize))
        val offset = hull.lb.finite
        for (FiniteIntInterval(l, u) <- d.contents) {
          bvb.set(Math.toIntExact(l - offset), Math.toIntExact(u - offset + 1))
        }
        new BitVectorDomain(Math.toIntExact(offset), BitVector(bvb), Math.toIntExact(size.finite))
      }
    }
  }

  def apply(r: Range): IntDomain =
    if (r.step == 1) {
      ofInterval(r.start, r.last)
    } else {
      val offset = r.start
      val builder = new IntDomainBuilder(offset)
      for (v <- r) builder += v - offset
      builder.result(r.size)
    }

  def ofBitVector(offset: Int, bv: BitVector, s: Int): IntDomain = s match {
    case 0 => EmptyIntDomain
    case 1 => Singleton(offset + bv.nextSetBit(0))
    case s: Int =>
      val lb = bv.nextSetBit(0)
      val ub = bv.lastSetBit
      assert(s == bv.cardinality)
      if (ub - lb == s - 1) {
        new IntervalDomain(offset + lb, offset + ub)
      } else if (lb >= BitVector.WORD_SIZE) {
        new BitVectorDomain(offset + lb, bv.shift(-lb), s)
      } else {
        new BitVectorDomain(offset, bv, s)
      }

  }

  def ofInterval(lb: Int, ub: Int): IntDomain =
    if (lb > ub) {
      EmptyIntDomain
    }
    else if (ub == lb) {
      Singleton(lb)
    }
    else {
      new IntervalDomain(lb, ub)
    }

  def apply(i: Interval): IntDomain = ofInterval(i.lb, i.ub)

  @annotation.varargs
  def ofSeq(d: Int*): IntDomain = ofTreeSet(TreeSet(d: _*))

  def ofTreeSet(set: TreeSet[Int]): IntDomain = {
    set.size match {
      case 0 => EmptyIntDomain
      case 1 => Singleton(set.head)
      case s: Int =>
        val lb = set.head
        val ub = set.last

        val span = ub - lb + 1
        if (span == s) {
          new IntervalDomain(lb, ub)
        } else if (s * TREE_SET_THRESHOLD < span) {
          new TreeSetDomain(set)
        } else {
          val bvb = new java.util.BitSet(span)
          for (i <- set) bvb.set(i - lb)
          new BitVectorDomain(lb, BitVector(bvb), s)
        }
    }
  }

}

abstract class IntDomain extends Domain {

  final def assign(value: Int): IntDomain = {
    assert(contains(value), s"tried to assign to $value which is not present")
    if (isAssigned) {
      this
    } else {
      Singleton(value)
    }
  }

  def |(value: Int): IntDomain

  def ++(values: Iterable[Int]): IntDomain = values.foldLeft(this)(_ | _)

  def removeItv(from: Int, to: Int): Domain = {
    val removed = removeFrom(from) | removeTo(to)
    if (removed.size == size) this else removed
  }

  override def find(f: Int => Boolean): Option[Int] = {
    if (isEmpty) None
    else {
      var current = head
      while (true) {
        if (f(current)) {
          return Some(current)
        } else if (current == last) {
          return None
        } else {
          current = next(current)
        }
      }
      throw new IllegalStateException()
    }
  }

}
