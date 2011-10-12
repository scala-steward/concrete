package cspfj;

import cspfj.generator.ProblemGenerator
import cspfj.problem.Problem
import cspom.compiler.ProblemCompiler
import cspom.constraint.CSPOMConstraint
import cspom.CSPOM
import java.util.logging.Level
import org.junit.Assert.{ assertTrue, assertNull, assertNotNull, assertEquals }
import org.junit.Test
import scala.collection.JavaConversions
import util.Loggable

final class SolvingTest extends Loggable {
  setLevel(Level.FINE)

  @Test
  def crosswordm1() {

    assert(solve("crossword-m1-debug-05-01.xml").isDefined);
    assertEquals(48, count("crossword-m1-debug-05-01.xml"));

  }

  @Test
  def crosswordm2() {
    assert(solve("crossword-m2-debug-05-01.xml").isDefined);
    assertEquals(48, count("crossword-m2-debug-05-01.xml"));

  }

  @Test
  def queens8() {
    assert(solve("queens-8.xml").isDefined);
    assertEquals(92, count("queens-8.xml"));

  }

  @Test
  def queens12_ext() {
    assert(solve("queens-12_ext.xml").isDefined);
    assertEquals(14200, count("queens-12_ext.xml"));

  }

  @Test
  def langford() {
    assert(solve("langford-2-4-ext.xml").isDefined);
    assertEquals(2, count("langford-2-4-ext.xml"));

  }

  @Test
  def zebra() {

    assert(solve("zebra.xml").isDefined);
    assertEquals(1, count("zebra.xml"));

  }

  @Test
  def dimacs() {
    assert(solve("flat30-1.cnf").isDefined);
    // assertNotNull(solve("clauses-2.cnf.bz2"));
    // assertEquals(1, count("flat30-1.cnf"));

  }

  @Test
  def bqwh() {
    assert(solve("bqwh-15-106-0_ext.xml").isDefined);
    assertEquals(182, count("bqwh-15-106-0_ext.xml"));
  }

  @Test
  def frb35_17_1() {
    // assertNotNull(solve("frb35-17-1_ext.xml.bz2"));
    assertEquals(2, count("frb35-17-1_ext.xml.bz2"));
  }

  @Test
  def scen11_f12() {
    assertEquals(solve("scen11-f12.xml.bz2"), None);
  }

  //  @Test
  //  def fapp01_0200_0() {
  //    assertNull(solve("fapp01-0200-0.xml"));
  //    assertEquals(0, count("fapp01-0200-0.xml"));
  //
  //  }

  @Test
  def queens12() {
    assert(solve("queens-12.xml").isDefined);
    //    assertEquals(14200, count("queens-12.xml"));

  }

  private def solve(name: String) = {
    val cspomProblem = CSPOM.load(getClass.getResource("problem/" + name));
    ProblemCompiler.compile(cspomProblem);
    val problem = ProblemGenerator.generate(cspomProblem);

    val solver = new MGACIter(problem);

    val itr = new SolverIterator(solver)

    if (itr.hasNext) {
      val sol = itr.next
      val failed = cspomProblem.controlInt(sol);
      assertTrue(failed.toString, failed.isEmpty)
      Some(sol)
    } else {
      None
    }

  }

  private def count(name: String) = {
    val cspomProblem = CSPOM.load(getClass.getResource("problem/" + name));
    ProblemCompiler.compile(cspomProblem);
    val problem = ProblemGenerator.generate(cspomProblem);
    // System.out.println(problem);

    val solver = new MGACIter(problem);
    var count = 0
    for (solution <- new SolverIterator(solver)) {
      count += 1
      val failed = cspomProblem.controlInt(solution)
      assertTrue(1 + count + "th solution: " + failed.toString(), failed.isEmpty);
    }

    count;
  }
}