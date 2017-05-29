package concrete.generator.cspompatterns

import cspom.CSPOM
import cspom.CSPOMConstraint
import cspom.compiler.ConstraintCompiler
import cspom.compiler.Delta
import cspom.extension.MDDRelation
import cspom.variable.SimpleExpression

object MergeRelations extends ConstraintCompiler {
  type A = (Boolean, Seq[CSPOMConstraint[_]])

  override def mtch(c: CSPOMConstraint[_], problem: CSPOM): Option[A] = {
    if (c.function == 'extension) {
      val init = c.getParam[Boolean]("init").get
      val head = c.arguments.head
      val mergeable = problem.constraints(head)
        .toSeq
        .filter(_.function == 'extension)
        .filter(_.getParam("init").contains(init))
        .filter(_.arguments == c.arguments)

      if (mergeable.size <= 1) None else Some((init, mergeable))
    } else {
      None
    }
  }

  def compile(c: CSPOMConstraint[_], problem: CSPOM, data: A): Delta = {
    // TODO : explodes. Disabled for now
    val (init, cons) = data
    
    //println(s"merging ${cons.mkString("\n")}")
    
    val relations = cons.map { c => c.getParam[MDDRelation]("relation").get }.map(_.mdd)

    val relation = if (init) relations.reduceLeft(_ union _) else relations.reduceLeft(_ intersect _)

    val args = c.arguments.map {
      case e: SimpleExpression[Int] => e
    }

    val nc = if (init) {
      CSPOM.IntSeqOperations(args) notIn new MDDRelation(relation)
    } else {
      CSPOM.IntSeqOperations(args) in new MDDRelation(relation)
    }

    replaceCtr(cons, nc, problem)
  }

  def selfPropagation = false

}