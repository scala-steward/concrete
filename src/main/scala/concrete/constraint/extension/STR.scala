package concrete.constraint.extension

import java.util

import concrete.Domain

import scala.annotation.tailrec
import scala.math.BigInt.int2bigInt

object STR {
  def apply(data: Array[Array[Int]]): STR = {
    // val d = data.toArray
    new STR(data, data.length)
  }
}

final class STR(val array: Array[Array[Int]], val bound: Int) extends Relation {
  type Self2 = STR

  private val pos: MutableList = new MutableList(depth)

  def copy = new STR(array.clone, bound)

  def +(t: Seq[Int]): STR = {
    assert(t.length == depth)
    new STR(t.toArray +: array, bound + 1)
  }

  override def ++(t: Iterable[Seq[Int]]): STR = {
    assert(t.forall(_.length == depth))
    new STR(t.map(_.toArray) ++: array, bound + t.size)
  }

  def depth: Int = array.headOption.map(_.length).getOrElse(0)

  def -(t: Seq[Int]) = throw new UnsupportedOperationException

  def filterTrie(doms: Array[Domain], modified: List[Int]): STR = {
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

  def supported(domains: Array[Domain]): Array[util.HashSet[Int]] = {

    val newDomains = Array.fill(domains.length)(new util.HashSet[Int]())
    pos.refill()

    var i = bound - 1
    while (i >= 0) {
      val tuple = array(i)
      pos.filter { p =>
        ReduceableExt.fills += 1
        newDomains(p).add(tuple(p))
        newDomains(p).size != domains(p).size
      }
      i -= 1
    }

    newDomains
  }

  override def toString: String = s"$bound of ${array.length} tuples:\n" + iterator.map(_.mkString(" ")).mkString("\n")

  def iterator: Iterator[Array[Int]] = array.iterator.take(bound)

  def edges: Int = if (array.isEmpty) 0 else bound * array(0).length

  def find(f: (Int, Int) => Boolean) = throw new UnsupportedOperationException

  def findSupport(scope: Array[Domain], p: Int, i: Int): Option[Array[Int]] =
    iterator.find(t => t(p) == i && t.indices.forall(p => scope(p).contains(t(p))))

  def contains(t: Array[Int]): Boolean = {
    var i = bound - 1
    while (i >= 0) {
      if (util.Arrays.equals(t, array(i))) return true
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

  override def size: Int = bound

  def lambda: BigInt = bound

  @tailrec
  private def valid(modified: List[Int], doms: Array[Domain], t: Array[Int]): Boolean = {
    modified.isEmpty || (doms(modified.head).contains(t(modified.head)) && valid(modified.tail, doms, t))
  }
}

final class MutableList(var nb: Int) extends Iterable[Int] {
  private val data = new Array[Int](size)

  def refill(): Unit = {
    nb = data.length
    var i = size - 1
    while (i >= 0) {
      data(i) = i
      i -= 1
    }
  }

  override def size: Int = nb

  def apply(index: Int): Int = data(index)

  override def foreach[U](f: Int => U): Unit = {
    var i = size - 1
    while (i >= 0) {
      f(data(i))
      i -= 1
    }
  }

  def iterator: Iterator[Int] = Iterator.range(0, size).map(data)

  override def filter(f: Int => Boolean): MutableList = {
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

  def remove(index: Int): Unit = {
    nb -= 1
    data(index) = data(size)
  }

  def sorted: Array[Int] = {
    val a = toArray
    util.Arrays.sort(a)
    a.reverse
  }

}
