package cspfj.generator.constraint;

import cspfj.constraint.semantic.Disj;
import cspfj.exception.FailedGenerationException;
import cspfj.problem.BitVectorDomain;
import cspfj.problem.Problem;
import cspfj.problem.Variable;
import cspom.constraint.CSPOMConstraint;
import cspom.constraint.FunctionalConstraint;
import cspom.constraint.GeneralConstraint;
import cspom.variable.CSPOMVariable;

public final class DisjGenerator extends AbstractGenerator {

	public DisjGenerator(Problem problem) {
		super(problem);
	}

	@Override
	public boolean generate(CSPOMConstraint constraint)
			throws FailedGenerationException {
		final Variable[] scope = getSolverVariables(constraint.getScope());

		for (Variable v : scope) {
			if (v.getDomain() == null) {
				v.setDomain(new BitVectorDomain(0, 1));
			}
		}

		if (constraint instanceof GeneralConstraint) {

			addConstraint(new Disj(scope));
			return true;

		} else if (constraint instanceof FunctionalConstraint) {

			/*
			 * Reified disjunction is converted to CNF :
			 * 
			 * a = b v c v d...
			 * 
			 * <=>
			 * 
			 * (-a v b v c v d...) ^ (a v -b) ^ (a v -c) ^ (a v -d) ^ ...
			 */
			final boolean[] reverses = new boolean[scope.length];
			reverses[0] = true;
			addConstraint(new Disj(scope, reverses));

			final FunctionalConstraint fConstraint = (FunctionalConstraint) constraint;
			final Variable result = getSolverVariable(fConstraint
					.getResultVariable());

			for (CSPOMVariable v : fConstraint.getArguments()) {
				addConstraint(new Disj(new Variable[] { result,
						getSolverVariable(v) }, new boolean[] { false, true }));
			}

			return true;

		} else {
			throw new IllegalArgumentException("Unhandled constraint type for "
					+ constraint);
		}
	}
}
