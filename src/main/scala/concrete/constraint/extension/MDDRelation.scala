package concrete.constraint.extension

import bitvectors.BitVector
import com.typesafe.scalalogging.LazyLogging
import concrete.{Domain, IntDomain}
import mdd.{MDD, MiniSet, SetWithMax}

object MDDRelation {
  def apply(data: Seq[Array[Int]]): MDDRelation =
    new MDDRelation(MDD.fromSeq(data))
}

final class MDDRelation(val mdd: MDD) extends Relation with LazyLogging {
  type Self2 = MDDRelation
  lazy val edges: Int = mdd.edges()
  lazy val lambda = mdd.lambda()

  def findSupport(domains: Array[Domain], p: Int, i: Int): Option[Array[Int]] = {
    assert(domains(p).present(i))
    val support = new Array[Int](domains.length)
    val s = mdd.findSupport(domains.asInstanceOf[Array[MiniSet]], p, i, support)
    assert(s.forall(contains))
    assert(s.forall { sup =>
      (sup zip domains).forall(a => a._2.present(a._1))
    })
    s
  }

  def contains(t: Array[Int]): Boolean = mdd.contains(t)

  def filterTrie(doms: Array[Domain], modified: List[Int]): MDDRelation = {
    val m = mdd.filterTrie(doms.asInstanceOf[Array[MiniSet]], modified)

//    assert(m.forall { sup =>
//      sup.zipWithIndex.forall {
//        case (i, p) =>
//          val r = doms(p).present(i)
//          if (!r) logger.warn(s"($p, $i) should have been filtered")
//          r
//      }
//    }, modified + "\n" + m)

    if (m eq mdd) {
      this
    } else {
      new MDDRelation(m)
    }
  }

  def supported(doms: Array[Domain]): Array[Domain] = {
    val newDomains = Array.fill[BitVector](doms.length)(BitVector.empty)
    val l = new SetWithMax(doms.length)
    mdd.supported(doms.asInstanceOf[Array[MiniSet]], newDomains, 0, l)
    newDomains.map(bv => IntDomain.ofBitVector(0, bv, bv.cardinality))
  }

  override def isEmpty = mdd.isEmpty

  override def toString = s"$edges e for $lambda t"

  // def copy: MDDRelation = new MDDRelation(mdd.copy(timestamp.next()))

  def depth = mdd.depth().get

  override def size = {
    val lambda = mdd.lambda()
    require(lambda.isValidInt)
    lambda.toInt
  }

  def identify() = mdd.identify()

  def iterator = mdd.iterator.map(_.toArray)

  def -(t: Seq[Int]) = throw new UnsupportedOperationException

  def +(t: Seq[Int]) = throw new UnsupportedOperationException //new MDDRelation(mdd + t)
}
