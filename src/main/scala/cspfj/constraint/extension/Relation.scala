package cspfj.constraint.extension

import cspfj.util.BitVector


trait Relation extends Iterable[Array[Int]] {
  type Self2 <: Relation
  def filterTrie(f: (Int, Int) => Boolean, modified: List[Int]): Self2
  
    /**
   * @param f(depth, i) : function called while traversing the trie, given depth and index. Returns true if traversing can be stopped at given level.
   * @param arity : arity of the relation
   * @return traversable of depths at which some values were not found
   */
  def fillFound(f: (Int, Int) => Boolean, arity: Int): Traversable[Int]
  
  
  def contains(t: Array[Int]): Boolean
  def find(f: (Int, Int) => Boolean): Option[Array[Int]]
  def +(t: Array[Int]): Self2
  def -(t: Array[Int]): Self2
  def ++(t: Iterable[Array[Int]]) = t.foldLeft(Relation.this)(_ + _)
  def --(t: Iterable[Array[Int]]) = t.foldLeft(Relation.this)(_ - _)
  def nodes: Int
  def copy: Self2
  def quickCopy: Self2
}