package concrete.longTest;

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import concrete.filter.DC1
import concrete.generator.FailedGenerationException
import concrete.generator.ProblemGenerator
import concrete.LearnMethod
import cspom.compiler.ProblemCompiler
import cspom.CSPOM
import concrete.util.Loggable
import concrete.ParameterManager
import concrete.Solver
import concrete.filter.SAC1
import concrete.MAC
import org.junit.After
import org.junit.Before

final class SACSolvingTest extends Loggable {
  //Solver.loggerLevel = "FINE"

  val dlog = Solver.loggerLevel

  @Before
  def before() {
    //Solver.loggerLevel = "INFO"
    ParameterManager("preprocessor") = classOf[SAC1]
    ParameterManager("closeRelations") = false
  }

  @After
  def after() {
    Solver.loggerLevel = dlog
    ParameterManager("preprocessor") = null
  }

  @Test
  def crosswordm1() {

    assertTrue(solve("crossword-m1-debug-05-01.xml").isDefined);
    //assertEquals(48, count("crossword-m1-debug-05-01.xml"));

  }

  @Test
  def crosswordm2() {
    assertTrue(solve("crossword-m2-debug-05-01.xml").isDefined);
    //assertEquals(48, count("crossword-m2-debug-05-01.xml"));

  }

  @Test
  def queens8() {
    assertTrue(solve("queens-8.xml").isDefined);
    assertEquals(92, count("queens-8.xml"));

  }

  @Test
  def queens12_ext() {
    assertTrue(solve("queens-12_ext.xml").isDefined);
    //assertEquals(14200, count("queens-12_ext.xml"));

  }

  @Test
  def langford() {
    assertTrue(solve("langford-2-4-ext.xml").isDefined);
    assertEquals(2, count("langford-2-4-ext.xml"));

  }

  @Test
  def zebra() {

    assertTrue(solve("zebra.xml").isDefined);
    assertEquals(1, count("zebra.xml"));

  }

  @Test
  def dimacs() {
    assertTrue(solve("flat30-1.cnf").isDefined);
    // assertNotNull(solve("clauses-2.cnf.bz2"));
    // assertEquals(1, count("flat30-1.cnf"));

  }

  @Test
  def bqwh() {
    assertTrue(solve("bqwh-15-106-0_ext.xml").isDefined);
    assertEquals(182, count("bqwh-15-106-0_ext.xml"));
  }

  @Test
  def frb35_17_1() {
    // assertNotNull(solve("frb35-17-1_ext.xml.bz2"));
    assertEquals(2, count("frb35-17-1_ext.xml.bz2"));
  }

  //  @Test
  //  def scen11_f12() {
  //    ParameterManager.parameter("ac.control", false)
  //    assertEquals(solve("scen11-f12.xml.bz2"), None);
  //    ParameterManager.parameter("ac.control", true)
  //  }

  //  @Test
  //  def fapp01_0200_0() {
  //    assertNull(solve("fapp01-0200-0.xml"));
  //    assertEquals(0, count("fapp01-0200-0.xml"));
  //
  //  }

  @Test
  def queens12() {
    assertTrue(solve("queens-12.xml").isDefined);
    //    assertEquals(14200, count("queens-12.xml"));

  }

  private def solve(name: String) = {
    val cspomProblem = CSPOM.load(getClass.getResource(name));
    ProblemCompiler.compile(cspomProblem);
    val problem = ProblemGenerator.generate(cspomProblem);

    val solver = new MAC(problem);

    solver.toIterable.headOption.map { sol =>
      val failed = cspomProblem.controlInt(sol);
      assertTrue(sol + "\n" + failed.toString, failed.isEmpty)
    }

  }

  private def count(name: String) = {
    val cspomProblem = CSPOM.load(getClass.getResource(name));
    ProblemCompiler.compile(cspomProblem);
    val problem = ProblemGenerator.generate(cspomProblem);
    // System.out.println(problem);

    val solver = new MAC(problem);

    var count = 0
    for (solution <- solver) {
      count += 1
      val failed = cspomProblem.controlInt(solution)
      assertTrue(1 + count + "th solution: " + failed.toString(), failed.isEmpty);
    }

    count;
  }
}