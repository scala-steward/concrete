package concrete
package constraint
package extension

import com.typesafe.scalalogging.LazyLogging
import mdd._
import java.util

final class BDDRelation(val bdd: BDD) extends Relation with LazyLogging {
  type Self2 = BDDRelation
  lazy val lambda: BigInt = bdd.lambda()

//  val offset : Int = {
//    def min(bdd: BDD, m: Int = Integer.MAX_VALUE, cache: TSSet[BDD] = new TSSet()): Int = {
//      bdd match {
//        case n: BDDNode =>
//          cache.onceOrElse(n, {
//            min(n.sibling, min(n.child, math.min(n.index, m), cache), cache)
//          }, m)
//        case _ => m
//      }
//    }
//    min(bdd)
//  }

  def findSupport(domains: Array[Domain], p: Int, i: Int): Option[Array[Int]] = {
    ???
  }

  def contains(t: Array[Int]): Boolean = bdd.contains(t)

  def vertices: Int = bdd.vertices()

  def depth: Int = bdd.depth(new IdMap())

  def filterTrie(doms: Array[Domain], modified: List[Int]): BDDRelation = {
    val m = bdd.filterTrie(doms.asInstanceOf[Array[MiniSet]], modified)

    //    assert(m.forall { sup =>
    //      sup.zipWithIndex.forall {
    //        case (i, p) =>
    //          val r = doms(p).present(i)
    //          if (!r) logger.warn(s"($p, $i) should have been filtered")
    //          r
    //      }
    //    }, modified + "\n" + m)

    if (m eq bdd) {
      this
    } else {
      new BDDRelation(m)
    }
  }

  override def supported(doms: Array[Domain]): Array[util.HashSet[Int]] = {
    bdd.supported(doms.asInstanceOf[Array[MiniSet]])
  }

  override def isEmpty = bdd.isEmpty

  override def toString = s"link, $edges e for $lambda t"

  def edges: Int = bdd.edges()

  // def copy: BDDRelation = this

  override def size = {
    require(lambda.isValidInt)
    lambda.toInt
  }

  def iterator = bdd.iterator.map(_.toArray)

  def -(t: Seq[Int]) = throw new UnsupportedOperationException

  def +(t: Seq[Int]) = new BDDRelation(bdd + t.toList)
}
