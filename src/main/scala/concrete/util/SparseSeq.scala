package concrete.util

import scala.collection.mutable.ArrayBuffer

object SparseSeq {
  def apply[A](a: A*): SparseSeq[A] = SparseSeq(ArrayBuffer(a:_*))

  def apply[A](a: ArrayBuffer[A]): SparseSeq[A] = new SparseSeq(a, a.length)
}

class SparseSeq[A](
                    val values: ArrayBuffer[A],
                    override val length: Int) extends Seq[A] {

  def this() = this(new ArrayBuffer[A](), 0)

  override def foreach[U](f: A => U): Unit = {
    var i = 0
    while (i < size) {
      f(values(i))
      i += 1
    }
  }

  def apply(i: Int): A = values(i)

  def copy = new SparseSeq(values.clone, size)

  def +(i: A): SparseSeq[A] = {
    values += i
    new SparseSeq(values, size + 1)
  }

  def partitionLeft(f: A => Boolean): (SparseSeq[A], Seq[A]) = {
    val bf = Seq.newBuilder[A]
    var newSize = size
    var i = newSize - 1
    while (i >= 0) {
      val v = values(i)
      if (!f(v)) {
        newSize -= 1
        values(i) = values(newSize)
        values(newSize) = v
        bf += v
      }
      i -= 1
    }
    (new SparseSeq(values, newSize), bf.result())
  }

  override def filter(f: A => Boolean): SparseSeq[A] = {

    var newSize = size
    var i = newSize - 1
    while (i >= 0) {
      val v = values(i)
      if (!f(v)) {
        newSize -= 1
        values(i) = values(newSize)
        values(newSize) = v
      }
      i -= 1
    }
    new SparseSeq(values, newSize)
  }

  override def isEmpty: Boolean = length == 0

  def iterator: Iterator[A] = values.iterator.take(size)

  override def lengthCompare(l: Int): Int = Ordering.Int.compare(length, l)

}