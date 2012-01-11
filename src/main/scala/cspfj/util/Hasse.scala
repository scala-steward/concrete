package cspfj.util

import scala.collection.immutable.BitSet
import scala.util.Random

case class HNode[A](
  val v: A,
  val child: List[HNode[A]]) {
  val rank: Int = if (child == Nil) 0 else 1 + child.map(_.rank).sum
}

object Hasse {
  def empty[A](po: PartialOrdering[A]) = new Hasse[A](po, Nil)
}

class Hasse[A](val po: PartialOrdering[A], val roots: List[HNode[A]]) {

  def +(v: A): Hasse[A] = new Hasse[A](po, add(v, roots))

  private def add(v: A, roots: List[HNode[A]]): List[HNode[A]] = {

    val (subsets, remaining) = roots.partition(r => po.lteq(r.v, v))

    if (subsets.isEmpty) {

      val (supersets, remaining) = roots.partition(r => po.lteq(v, r.v))
      if (supersets.isEmpty) {
        HNode(v, collect(v, remaining, Set.empty).toList) :: remaining
      } else {
        remaining ::: supersets map { r => HNode(r.v, add(v, r.child)) }
      }
      
    } else {
      HNode(v, subsets ::: collect(v, remaining, Set.empty).toList) :: remaining
    }

  }

  private def collect(v: A, roots: List[HNode[A]], collected: Set[HNode[A]]): Set[HNode[A]] = {
    if (roots == Nil) collected
    else {
      val (subsets, remaining) = roots.partition(r => po.lteq(r.v, v))
      collect(v, remaining.flatMap(r => r.child.filter(!collected(_))), collected ++ subsets)
    }
  }

  override def toString: String = roots.toString

  def toGML = {
    val stb = new StringBuilder();
    stb.append("graph [\n");
    stb.append("directed 0\n");

    def flatnodes(r: List[HNode[A]]): Set[A] = r.map(_.v).toSet ++ r.flatMap(v => flatnodes(v.child))

    for (v <- flatnodes(roots)) {
      stb.append("node [\n");
      stb.append("id \"").append(v.toString).append("\"\n");
      stb.append("label \"").append(v.toString).append("\"\n");
      stb.append("]\n");
    }

    def edges(s: HNode[A], r: List[HNode[A]]): String = (if (s != null) r.map { n =>
      val stb = new StringBuilder
      stb.append("edge [\n");
      stb.append("source \"").append(s.v).append("\"\n");
      stb.append("target \"").append(n.v.toString).append("\"\n");
      stb.append("]\n");
      stb.toString
    }.foldLeft("")(_ + _)
    else "") + (r.flatMap(n => edges(n, n.child)).foldLeft("")(_ + _))

    stb.append(edges(null, roots))
    stb.append("]\n").toString
  }

}

trait PredefPO[A] extends PartialOrdering[A] {
  def tryCompare(a: A, b: A) =
    if (lteq(a, b)) {
      if (lteq(b, a)) Some(0)
      else Some(-1)
    } else None
}

class SetInclusion[A] extends PredefPO[Set[A]] {
  def lteq(a: Set[A], b: Set[A]) = a.subsetOf(b)
}

object Test {
  val rand = new Random(0)

  def randSet() = {
    BitSet.empty ++ Stream.continually(rand.nextInt(5)).take(rand.nextInt(5))
  }

  def main(a: Array[String]) {

    var h: Hasse[(Int, Set[Int])] = Hasse.empty(new PredefPO[(Int, Set[Int])] {
      val si = new SetInclusion[Int]

      override def lteq(a: (Int, Set[Int]), b: (Int, Set[Int])) = si.lteq(a._2, b._2)
    })

    var i = 0
    while (i < 10) {
      val s = randSet()
      h += (i, s)
      println((i, s))
      i += 1
    }
    println(h)
    println(h.toGML)
  }
}
