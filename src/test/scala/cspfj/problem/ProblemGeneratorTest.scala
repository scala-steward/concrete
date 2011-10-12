package cspfj.problem;

import java.io.IOException
import java.util.logging.Logger
import java.util.logging.Level.WARNING
import org.junit.Test
import cspfj.filter.AC3Constraint
import cspfj.generator.ProblemGenerator
import cspfj.util.Loggable
import cspom.CSPOM
import cspom.compiler.ProblemCompiler;

final class ProblemGeneratorTest extends Loggable {

  setLevel(WARNING)

  @Test
  def zebra() {
    generateTest("zebra.xml");
  }

  @Test
  def queens4() {
    generateTest("queens-4.xml");
  }

  @Test
  def queens12() {
    generateTest("queens-12.xml");
  }

  @Test
  def queens20() {
    generateTest("queens-20.xml.bz2");
  }

  @Test
  def queens50() {
    generateTest("queens-50.xml.bz2");
  }

  @Test
  def scen11f10() {
    generateTest("scen11-f10.xml.bz2");
  }

  @Test
  def crosswordm2() {
    generateTest("crossword-m2-debug-05-01.xml");
  }

  @Test
  def lexHerald() {
    generateTest("normalized-crossword-m1-lex-15-04.xml.bz2");
  }

  // @Test
  // public void fapp01_0200_0() throws CSPParseException, IOException,
  // FailedGenerationException, ClassNotFoundException {
  // generateTest("fapp01-0200-0.xml");
  // }

  private def generateTest(file: String) {
    val cspom = CSPOM.load(classOf[ProblemGeneratorTest].getResource(file));

    info(cspom + "\n" + cspom.variables.size + " vars, " + cspom.constraints.size + " cons")

    ProblemCompiler.compile(cspom);

    info(cspom + "\n" + cspom.variables.size + " vars, " + cspom.constraints.size + " cons")

    val problem = ProblemGenerator.generate(cspom);

    info(problem + "\n" + problem.variables.size + " vars, " + problem.constraints.size + " cons")

    new AC3Constraint(problem).reduceAll();

    info(problem.toString);
  }
}