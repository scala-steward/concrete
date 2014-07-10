package concrete.util

final case class Interval(val lb: Int, val ub: Int) {
  assume(ub >= lb)
  val size = ub - lb + 1
  def contains(v: Int) = lb <= v && v <= ub

  def allValues = lb to ub

  /**
   * [a, b] + [c, d] = [a + c, b + d]
   * [a, b] − [c, d] = [a − d, b − c]
   * [a, b] × [c, d] = [min (a × c, a × d, b × c, b × d), max (a × c, a × d, b × c, b × d)]
   * [a, b] ÷ [c, d] = [min (a ÷ c, a ÷ d, b ÷ c, b ÷ d), max (a ÷ c, a ÷ d, b ÷ c, b ÷ d)] when 0 is not in [c, d].
   */

  def +(i: Interval) = Interval(lb + i.lb, ub + i.ub)

  def +(v: Int) = Interval(lb + v, ub + v)

  def -(i: Interval) = Interval(lb - i.ub, ub - i.lb)

  def -(v: Int) = this + -v

  def *(i: Interval) = {
    val Interval(c, d) = i
    val prods = List(lb * c, lb * d, ub * c, ub * d)
    Interval(prods.min, prods.max)
  }

  def *(v: Int) = {
    val lbv = lb * v
    val ubv = ub * v
    if (lbv < ubv) {
      Interval(lbv, ubv)
    } else {
      Interval(ubv, lbv)
    }
  }

  def sq() = {
    val lb2 = lb * lb
    val ub2 = ub * ub
    if (contains(0)) {
      Interval(0, math.max(lb2, ub2))
    } else {
      if (lb2 < ub2) {
        Interval(lb2, ub2)
      } else {
        Interval(ub2, lb2)
      }
    }
  }

  def sqrt() = {
    require(lb >= 0)
    val root = math.sqrt(ub).toInt
    Interval(-root, root)
  }

  def /(i: Interval) = {
    if (i.contains(0)) throw new ArithmeticException
    val Interval(c, d) = i
    Interval(
      List(ceilDiv(lb, c), ceilDiv(lb, d), ceilDiv(ub, c), ceilDiv(ub, d)).min,
      List(floorDiv(lb, c), floorDiv(lb, d), floorDiv(ub, c), floorDiv(ub, d)).max)
  }

  def /(v: Int) = {
    if (v >= 0) {
      Interval(ceilDiv(lb, v), floorDiv(ub, v))
    } else {
      Interval(ceilDiv(ub, v), floorDiv(lb, v))
    }

    //    if (l < u) Interval(l, u) else Interval(u, l)
  }

  def /:(v: Int) = {
    /** v / this **/
    if (this.contains(0)) throw new ArithmeticException
    Interval(
      math.min(ceilDiv(v, lb), ceilDiv(v, ub)),
      math.max(floorDiv(v, lb), floorDiv(v, ub)))
  }

  def floorDiv(dividend: Int, divisor: Int) = {
    val roundedTowardsZeroQuotient = dividend / divisor;
    val dividedEvenly = (dividend % divisor) == 0;
    if (dividedEvenly) {
      roundedTowardsZeroQuotient;
    } else {
      // If they're of opposite sign then we rounded 
      // UP towards zero so we rem one. If they're of the same sign then 
      // we rounded DOWN towards zero, so we are done.

      if (divisor.signum == dividend.signum) {
        roundedTowardsZeroQuotient;
      } else {
        roundedTowardsZeroQuotient - 1;
      }
    }
  }

  def ceilDiv(dividend: Int, divisor: Int) = {

    val roundedTowardsZeroQuotient = dividend / divisor;
    val dividedEvenly = (dividend % divisor) == 0;
    if (dividedEvenly) {
      roundedTowardsZeroQuotient;
    } else {
      // If they're of opposite sign then we rounded 
      // UP towards zero so we're done. If they're of the same sign then 
      // we rounded DOWN towards zero, so we need to add one.

      if (divisor.signum == dividend.signum) {
        roundedTowardsZeroQuotient + 1;
      } else {
        roundedTowardsZeroQuotient;
      }
    }
  }

  def intersect(i: Interval) = {
    val l = math.max(lb, i.lb)
    val u = math.min(ub, i.ub)
    if (l <= u) { Some(Interval(l, u)) }
    else { None }
  }

  def union(i: Interval) = Interval(math.min(lb, i.lb), math.max(ub, i.ub))

  def negate = Interval(-ub, -lb)

  def abs =
    if (ub < 0) { negate }
    else if (lb > 0) { this }
    else { Interval(0, math.max(-lb, ub)) }

}
