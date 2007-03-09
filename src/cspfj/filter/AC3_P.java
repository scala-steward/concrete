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

package cspfj.filter;

import java.util.Arrays;

import cspfj.constraint.Constraint;
import cspfj.problem.Problem;
import cspfj.problem.Variable;

/**
 * @author scand1sk
 * 
 */
public final class AC3_P implements Filter {

	// private static final Logger logger = Logger.getLogger("cspfj.AC3");

	private final Problem problem;

	// private final Maximier<Variable> queue;

	private final boolean[] inQueue;

	private int queueSize = 0;

	// private final Map<Integer, boolean[]> inQueue;

	public AC3_P(final Problem problem) {
		super();
		this.problem = problem;

		// inQueue = new HashMap<Integer,
		// boolean[]>(problem.getNbConstraints());

		// int size = 0;
		// for (Constraint c : problem.getConstraints()) {
		// size += c.getArity();
		// inQueue.put(c.getId(), new boolean[c.getArity()]);
		// }

		inQueue = new boolean[problem.getMaxVId() + 1];

		// queue = new Maximier<Variable>(new
		// Variable[problem.getNbVariables()]);
	}

	public boolean reduceAll(final int level) {
		clearQueue();
		addAll();
		return reduce(level);

	}

	public boolean reduceAfter(final int level, final Variable variable) {
		if (variable == null) {
			return true;
		}
		clearQueue();

		addInQueue(variable.getId());

		// addNeighbours(variable);

		// variable.setNbRemovals(1) ;

		return reduce(level);
	}

	private boolean skipRevision(final Constraint constraint, final int variablePosition) {
		if (!constraint.getRemovals(variablePosition)) {
			return false;
		}

		for (int y = constraint.getArity(); --y >= 0;) {
			if (y != variablePosition && constraint.getRemovals(y)) {
				return false;
			}

		}

		return true;
	}

	private boolean reduce(final int level) {
		// logger.fine("AC...");
		while (queueSize > 0) {
			// logger.fine(""+queueSize);
			// System.err.println(queueSize);
			final Variable variable = pullVariable();

			for (Constraint c : variable.getInvolvingConstraints()) {

				if (!c.getRemovals(variable)) {
					continue;
				}

				for (int i = c.getArity(); --i >= 0;) {
					final Variable y = c.getInvolvedVariables()[i];

					if (!y.isAssigned() && !skipRevision(c, i)
							&& c.revise(i, level)) {
						if (y.getDomainSize() <= 0) {
							c.increaseWeight();
							return false;
						}

						addInQueue(y.getId());

						for (Constraint cp : y.getInvolvingConstraints()) {
							if (cp != c) {
								cp.setRemovals(y, true);
							}
						}
					}
				}

				c.fillRemovals(false);
			}

		}

		return true;

	}

	private void clearQueue() {
		Arrays.fill(inQueue, false);
		queueSize = 0;
		for (Constraint c : problem.getConstraints()) {
			c.fillRemovals(true);
		}
	}

	private void addAll() {
		for (Variable v : problem.getVariables()) {
			addInQueue(v.getId());
		}
	}

	private Variable pullVariable() {
		Variable bestVariable = null;
		int bestValue = Integer.MAX_VALUE;

		final boolean[] inQueue = this.inQueue;

		final Problem problem = this.problem;

		for (int i = inQueue.length; --i >= 0;) {
			if (inQueue[i]) {
				final Variable variable = problem.getVariable(i);
				final int domainSize = variable.getDomainSize();
				if (domainSize < bestValue) {
					bestVariable = variable;
					bestValue = domainSize;
				}
			}
		}

		inQueue[bestVariable.getId()] = false;
		queueSize--;
		return bestVariable;
	}

	private void addInQueue(final int vId) {
		if (!inQueue[vId]) {
			inQueue[vId] = true;
			queueSize++;
		}
	}

	// private void addNeighbours(final Variable variable) {
	// for (Variable n : variable.getNeighbours()) {
	// addInQueue(n);
	// }
	// }

	public int getNbNoGoods() {
		return 0;
	}

	public String toString() {
		return "AC3rm";
	}
	// public int getNbSub() {
	// return 0;
	// }

}
