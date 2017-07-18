package concrete.constraint.extension

import java.util.Arrays
import scala.annotation.tailrec
import scala.math.BigInt.int2bigInt
import concrete.Domain
import concrete.IntDomain
import concrete.EmptyIntDomain

object STR  {
  def apply(data: Seq[Array[Int]]): STR = {
    val d = data.toArray
    new STR(d, d.length)
  }
}

final class STR(val array: Array[Array[Int]], val bound: Int) extends Relation {
  type Self2 = STR

  def copy = new STR(array.clone, bound)

  def +(t: Seq[Int]) = {
    assert(t.length == depth)
    new STR(t.toArray +: array, bound + 1)
  }

  override def ++(t: Iterable[Seq[Int]]) = {
    assert(t.forall(_.length == depth))
    new STR(t.map(_.toArray) ++: array, bound + t.size)
  }

  def -(t: Seq[Int]) = throw new UnsupportedOperationException

  def filterTrie(doms: Array[Domain], modified: List[Int]) = {
    var b = bound
    var i = b - 1
    while (i >= 0) {
      if (!valid(modified, doms, array(i))) {
        b -= 1
        val tmp = array(i)
        array(i) = array(b)
        array(b) = tmp
      }
      i -= 1
    }
    if (b == bound) {
      this
    } else {
      new STR(array, b)
    }
  }

  @tailrec
  private def valid(modified: List[Int], doms: Array[Domain], t: Array[Int]): Boolean = {
    modified.isEmpty || (doms(modified.head).present(t(modified.head)) && valid(modified.tail, doms, t))
  }

  private val pos: MutableList = new MutableList(depth)

  def supported(domains: Array[Domain]): Array[Domain] = {

    val newDomains = Array.fill[IntDomain](domains.length)(EmptyIntDomain)
    pos.refill()

    var i = bound - 1
    while (i >= 0) {
      val tuple = array(i)
      pos.filter { p =>
        ReduceableExt.fills += 1
        newDomains(p) |= tuple(p)
        newDomains(p).size != domains(p).size
      }
      i -= 1
    }

    newDomains.asInstanceOf[Array[Domain]]
  }

  override def toString = s"$bound of ${array.size} tuples:\n" + iterator.map(_.mkString(" ")).mkString("\n")

  def edges = if (array.isEmpty) 0 else bound * array(0).length

  def find(f: (Int, Int) => Boolean) = throw new UnsupportedOperationException

  def findSupport(scope: Array[Domain], p: Int, i: Int) =
    iterator.find(t => t(p) == i && t.indices.forall(p => scope(p).present(t(p))))

  def iterator = array.iterator.take(bound)

  def contains(t: Array[Int]): Boolean = {
    var i = bound - 1
    while (i >= 0) {
      if (Arrays.equals(t, array(i))) return true
      i -= 1
    }
    false
  }

  def universal(scope: IndexedSeq[Domain]): Boolean = {
    var card = 1.0 / size
    var i = scope.length - 1
    while (i >= 0) {
      card *= scope(i).size
      if (card > 1.0) {
        return false
      }
      i -= 1
    }
    true
  }

  override def size = bound
  def lambda = bound
  def depth: Int = array.headOption.map(_.length).getOrElse(0)
}

final class MutableList(var nb: Int) extends Traversable[Int] {
  private val data = new Array[Int](size)

  override def size = nb

  def refill() {
    nb = data.length
    var i = size - 1
    while (i >= 0) {
      data(i) = i
      i -= 1
    }
  }

  def apply(index: Int) = data(index)

  def remove(index: Int) {
    nb -= 1
    data(index) = data(size)
  }

  def foreach[U](f: Int => U) {
    var i = size - 1
    while (i >= 0) {
      f(data(i))
      i -= 1
    }
  }

  override def filter(f: Int => Boolean) = {
    var i = 0
    while (i < size) {
      if (!f(data(i))) {
        remove(i)
      } else {
        i += 1
      }
    }
    this
  }

  def sorted = {
    val a = toArray
    Arrays.sort(a)
    a.reverse
  }

}
