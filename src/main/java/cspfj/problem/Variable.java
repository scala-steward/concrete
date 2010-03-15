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

package cspfj.problem;

import java.util.ArrayList;
import java.util.List;

import cspfj.constraint.Constraint;
import cspfj.constraint.DynamicConstraint;
import cspfj.generator.DomainGenerator;
import cspfj.priorityqueues.Identified;
import cspfj.util.BitVector;
import cspom.variable.CSPOMVariable;

public final class Variable implements Cloneable, Identified {

	private Constraint[] constraints;

	private DynamicConstraint[] dynamicConstraints;

	private Domain domain;

	private boolean assigned;

	private static int nbV = 0;

	private final int id;

	private int[] positionInConstraint;

	private final CSPOMVariable cspomVariable;

	public Variable(final CSPOMVariable cspomVariable) {
		this.cspomVariable = cspomVariable;
		domain = DomainGenerator.generate(cspomVariable.getDomain());
		assigned = false;
		id = nbV++;
	}

	/**
	 * Reinit ID generator (before loading a new problem).
	 */
	public static void resetVId() {
		nbV = 0;
	}

	@Override
	public String toString() {
		if (domain == null) {
			return cspomVariable.getName() + " [?]";
		}
		return cspomVariable.getName() + " [" + getDomainSize() + "]";
	}

	/**
	 * @param constraints2
	 *            Liste des contraintes impliquant la variable
	 */
	public void setInvolvingConstraints(final Constraint[] constraints2) {
		this.constraints = constraints2;
		final List<DynamicConstraint> dynamicConstraints = new ArrayList<DynamicConstraint>();
		for (Constraint c : constraints2) {
			if (c instanceof DynamicConstraint) {
				dynamicConstraints.add((DynamicConstraint) c);
			}
		}

		this.dynamicConstraints = dynamicConstraints
				.toArray(new DynamicConstraint[dynamicConstraints.size()]);

		positionInConstraint = new int[constraints2.length];
		for (int i = constraints2.length; --i >= 0;) {
			updatePositionInConstraint(i);
		}

	}

	public void updatePositionInConstraint(final int constraintPosition) {
		positionInConstraint[constraintPosition] = constraints[constraintPosition]
				.getPosition(this);
	}

	/**
	 * @return La taille du domaine
	 */
	public int getDomainSize() {
		if (domain == null) {
			return cspomVariable.getDomain().getValues().size();
		}
		return domain.size();
	}

	public boolean isAssigned() {
		return assigned;
	}

	public void assign(final int index, final Problem problem) {
		assert !assigned;
		assert domain.present(index);

		assigned = true;
		domain.setSingle(index);

		problem.decreaseFutureVariables();
	}

	public void unassign(final Problem problem) {
		assert assigned;
		assigned = false;
		problem.increaseFutureVariables();
	}

	public void remove(final int index) {
		assert !assigned : "Trying to remove a value from an assigned variable";
		assert domain.present(index);

		domain.remove(index);
	}

	public int getId() {
		return id;
	}

	public Constraint[] getInvolvingConstraints() {
		return constraints;
	}

	public DynamicConstraint[] getDynamicConstraints() {
		return dynamicConstraints;
	}

	public Domain getDomain() {
		return domain;
	}

	public int getFirst() {
		return domain.first();
	}

	public int getLast() {
		return domain.last();
	}

	public void setDomain(final Domain domain) {
		this.domain = domain;
	}

	public int[] getCurrentIndexes() {
		if (domain == null) {
			return null;
		}
		final int[] indexes = new int[domain.size()];

		for (int i = getFirst(), j = 0; i >= 0; i = getNext(i), j++) {
			indexes[j] = i;
		}

		return indexes;
	}

	public int getNext(final int index) {
		return domain.next(index);
	}

	public int getPrev(final int index) {
		return domain.prev(index);
	}

	public int getLastAbsent() {
		return domain.lastAbsent();
	}

	public int getPrevAbsent(final int index) {
		return domain.prevAbsent(index);
	}

	public BitVector getBitDomain() {
		return ((BitVectorDomain) domain).getBitVector();
	}

	public Variable clone() throws CloneNotSupportedException {
		final Variable variable = (Variable) super.clone();

		variable.domain = domain.clone();

		return variable;
	}

	public int getPositionInConstraint(final int constraint) {
		return positionInConstraint[constraint];
	}

	public void setLevel(int level) {
		domain.setLevel(level);
	}

	public void restoreLevel(int level) {
		domain.restoreLevel(level);
	}

	public void reset(Problem problem) {
		if (isAssigned()) {
			unassign(problem);
		}
		domain.restoreLevel(0);
	}

	public boolean isPresent(int index) {
		return domain.present(index);
	}

	public int getValue(int index) {
		return domain.value(index);
	}

	public void makeSingleton(int value1) {
		domain.setSingle(value1);
	}
}
