package concrete;

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test
import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ParameterManagerTester(private val pm: ParameterManager) {
  val classTest = pm("classTest").getOrElse(classOf[Int])

  val intTest = pm("intTest").getOrElse(12)
}

final class ParameterManagerTest extends FlatSpec with Matchers {

  val pm = new ParameterManager

  "A ParameterManager" should "handle classes" in {
    new ParameterManagerTester(pm).classTest shouldBe classOf[Int]
    pm("classTest") = classOf[Double]
    new ParameterManagerTester(pm).classTest shouldBe classOf[Double]
    pm("classTest") = "java.math.BigInteger"
    new ParameterManagerTester(pm).classTest shouldBe classOf[BigInteger]
  }

  it should "handle ints" in {
    new ParameterManagerTester(pm).intTest shouldBe 12
    pm("intTest") = 32
    new ParameterManagerTester(pm).intTest shouldBe 32
  }

}
