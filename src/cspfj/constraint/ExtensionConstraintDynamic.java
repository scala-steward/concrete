/**
 * CSPFJ - CSP solving API for Java
 * Copyright (C) 2006 Julien VION
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cspfj.constraint;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.logging.Logger;

import cspfj.constraint.MatrixManagerDynamic.LLIterator;
import cspfj.exception.FailedGenerationException;
import cspfj.filter.RevisionHandler;
import cspfj.problem.Variable;

public class ExtensionConstraintDynamic extends AbstractConstraint implements
		DynamicConstraint {

	private final MatrixManagerDynamic dynamic;

	private final BitSet[] found;

	private final static Logger logger = Logger
			.getLogger(ExtensionConstraintDynamic.class.getSimpleName());

	public ExtensionConstraintDynamic(final Variable[] scope,
			final TupleHashSet matrix, final boolean shared) throws FailedGenerationException {
		super(scope);
		this.dynamic = new MatrixManagerDynamic(scope, matrix, shared);
		found = initFound();
	}

	public ExtensionConstraintDynamic(final Variable[] scope,
			final TupleHashSet matrix, final String name, final boolean shared)
			throws FailedGenerationException {
		super(scope, name);
		this.dynamic = new MatrixManagerDynamic(scope, matrix, shared);
		found = initFound();
	}

	private BitSet[] initFound() {

		final BitSet[] found = new BitSet[getArity()];
		for (int i = getArity(); --i >= 0;) {
			found[i] = new BitSet(getVariable(i).getDomain().length);
		}
		return found;
	}

	public void restore(final int level) {
		dynamic.restore(level);
	}

	public boolean revise(final int level, final RevisionHandler revisator) {
		// logger.fine("Revising "+this);
		// int toFind = 0;
		for (int i = getArity(); --i >= 0;) {
			found[i].set(0, getVariable(i).getDomain().length);

			if (!getVariable(i).isAssigned()) {
				for (int index = getVariable(i).getFirst(); index >= 0; index = getVariable(
						i).getNext(index)) {
					found[i].clear(index);
					// toFind++;
				}
			}
		}

		final LLIterator itr = dynamic.iterator();

		while (itr.hasNext()) {
			final int[] tuple = itr.next();
			if (tuple == null) {
				continue;
			}
			if (controlTuplePresence(tuple)) {
				for (int i = getArity(); --i >= 0;) {
					found[i].set(tuple[i]);
				}
			} else {
				itr.remove(level);
			}
		}

		for (int i = getArity(); --i >= 0;) {
			final Variable variable = getVariable(i);

			boolean rev = false;
			final BitSet found = this.found[i];
			for (int index = variable.getFirst(); index >= 0; index = variable
					.getNext(index)) {

				if (!found.get(index)) {
					// logger.finer("removing " + index + " from " + variable);

					variable.remove(index, level);

					rev = true;

				}

			}
			if (rev) {
				if (variable.getDomainSize() <= 0) {
					return false;
				}
				revisator.revised(this, variable);
				setActive(true);
			}
		}

		return true;
	}

	private boolean controlTuplePresence(final int[] tuple) {
		if (tuple.length != getArity()) {
			return false;
		}
		for (int i = getArity(); --i >= 0;) {
			if (tuple == null || !getVariable(i).isPresent(tuple[i])) {
				return false;
			}
		}
		return true;
	}

	public ExtensionConstraintDynamic deepCopy(
			final Collection<Variable> variables)
			throws CloneNotSupportedException {
		final ExtensionConstraintDynamic constraint = (ExtensionConstraintDynamic) super
				.deepCopy(variables);
		// constraint.matrix = matrix.deepCopy(constraint.getScope(),
		// constraint.tuple);
		return constraint;
	}

	public void initNbSupports() throws InterruptedException {
		// logger.fine("Counting " + this + " supports");

		final long[][] nbInitConflicts = new long[getArity()][];
		for (int i = getArity(); --i >= 0;) {
			nbInitConflicts[i] = new long[getScope()[i].getDomain().length];
		}
		final long[] nbMaxConflicts = new long[getArity()];

		
		final long size = currentSize();
		if (size < 0) {
			return;
		}

		Arrays.fill(nbMaxConflicts, 0);
		for (int p = getArity(); --p >= 0;) {
			Arrays.fill(nbInitConflicts[p], size
					/ getVariable(p).getDomainSize());
		}

		for (int[] tuple : dynamic) {
			if (Thread.interrupted()) {
				logger.fine("Interrupted");
				throw new InterruptedException();
			}
			for (int p = getArity(); --p >= 0;) {
				nbInitConflicts[p][tuple[p]]--;
			}
		}

		for (int p = getArity(); --p >= 0;) {
			final Variable variable = getVariable(p);
			long max = 0;
			final long[] hereConflicts = nbInitConflicts[p];
			for (int i = variable.getFirst(); i != -1; i = variable.getNext(i)) {
				if (max < hereConflicts[i]) {
					max = hereConflicts[i];
				}
			}
			nbMaxConflicts[p] = max;
		}
		this.nbInitConflicts = nbInitConflicts;
		this.nbMaxConflicts = nbMaxConflicts;
	}

	@Override
	public boolean isSlow() {
		return true;
	}

	@Override
	public boolean check() {
		return dynamic.check();
	}

	@Override
	public boolean removeTuple(int[] tuple) {
		return dynamic.removeTuple(tuple);
	}
}