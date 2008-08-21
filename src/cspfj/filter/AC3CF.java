package cspfj.filter;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import cspfj.constraint.Constraint;
import cspfj.heuristic.VariableHeuristic;
import cspfj.problem.Problem;
import cspfj.problem.Variable;
import cspfj.util.Heap;

/**
 * @author scand1sk
 * 
 */
public final class AC3CF implements Filter {
	private final Problem problem;

	private final Deque<Constraint> queue;

	// private int queueSize = 0;

	private final boolean[] cstInQueue;

	public int effectiveRevisions = 0;

	public int uselessRevisions = 0;

	private static final Logger logger = Logger.getLogger(Filter.class
			.getSimpleName());

	public AC3CF(final Problem problem, final VariableHeuristic heuristic) {
		super();
		this.problem = problem;

		cstInQueue = new boolean[problem.getMaxCId() + 1];

		queue = new ArrayDeque<Constraint>(problem.getNbConstraints());

		for (Constraint c : problem.getConstraints()) {
			c.fillRemovals(true);
		}
		// queue = new ArrayDeque<Constraint>();
	}

	public boolean reduceAll(final int level) {
		queue.clear();
		queue.addAll(problem.getConstraintsAsCollection());
		Arrays.fill(cstInQueue, true);
		return reduce(level);
	}


	public boolean reduceAfter(final int level, final Variable variable) {
		if (variable == null) {
			return true;
		}
		queue.clear();
		Arrays.fill(cstInQueue, false);

		for (Constraint c : variable.getInvolvingConstraints()) {
			cstInQueue[c.getId()] = true;
			queue.add(c);

		}
		return reduce(level);
	}

	private RevisionHandler revisionHandler = new RevisionHandler() {

		@Override
		public void revised(Constraint constraint, Variable variable) {
			for (Constraint c : variable.getInvolvingConstraints()) {
				if (c != constraint) {
					if (!cstInQueue[c.getId()]) {
						cstInQueue[c.getId()] = true;
						queue.add(c);
					}
				}
			}
		}

	};

	private boolean reduce(final int level) {
		logger.fine("Reducing");

		final RevisionHandler revisionHandler = this.revisionHandler;
		final Deque<Constraint> queue = this.queue;

		while (!queue.isEmpty()) {
			Constraint constraint = queue.pollFirst();

			cstInQueue[constraint.getId()] = false;

			if (!constraint.revise(level, revisionHandler)) {
				constraint.incWeight();
				return false;
			}

		}

		return true;

	}





	public String toString() {
		return "GAC3rm-constraint";
	}

	public Map<String, Object> getStatistics() {
		final Map<String, Object> statistics = new HashMap<String, Object>();
		statistics.put("gac3-effective-revisions", effectiveRevisions);
		statistics.put("gac3-useless-revisions", uselessRevisions);
		return statistics;
	}

	@Override
	public boolean ensureAC() {
		return true;
	}

}
