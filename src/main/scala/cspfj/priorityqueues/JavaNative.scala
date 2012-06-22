package cspfj.priorityqueues
import java.util.Comparator

class JavaNative[T <: PTag] extends PriorityQueue[T] {

  case class E(val e: T, val eval: Int) extends Ordered[E] {
    def compare(y: E) = eval.compare(y.eval)
  }

  val queue = new java.util.PriorityQueue[E]()

  def offer(elt: T, eval: Int) = {
    if (elt.isPresent) {
      false
    } else {
      queue.offer(E(elt, eval))
      elt.setPresent()
      true
    }
  }

  def poll() = {
    val elt = queue.poll().e
    elt.unsetPresent
    elt
  }

  def peek = queue.peek

  def size = queue.size

  override def clear() {
    queue.clear()
    PTag.clear()
  }

  def iterator = queue.iterator

}