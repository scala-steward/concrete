package cspfj.heuristic;

import cspfj.problem.Problem;
import cspfj.problem.Variable;

public class DiscHeuristic implements Heuristic {

	private final Variable[] variables;

	private final VariableHeuristic variableHeuristic;

	private final ValueHeuristic valueHeuristic;

	public DiscHeuristic(Problem problem, VariableHeuristic variableHeuristic,
			ValueHeuristic valueHeuristic) {
		this.variableHeuristic = variableHeuristic;
		this.valueHeuristic = valueHeuristic;
		this.variables = problem.getVariables();
	}

	public long selectPair() {
		Variable bestVariable = null;
		double bestIndexScore = Double.NEGATIVE_INFINITY;

		for (Variable v : variables) {
			if (v.getDomainSize() <= 1) {
				continue;
			}

			final int compare = variableHeuristic.compare(v, bestVariable);

			if (compare < 0) {
				bestVariable = v;
				bestIndexScore = Double.NEGATIVE_INFINITY;
			} else if (compare == 0) {
				if (bestIndexScore == Double.NEGATIVE_INFINITY) {
					bestIndexScore = valueHeuristic.getScore(bestVariable,
							valueHeuristic.selectIndex(bestVariable));
				}
				final double indexScore = valueHeuristic.getScore(v,
						valueHeuristic.selectIndex(v));
				if (indexScore > bestIndexScore) {
//					System.out.print("disc! ");
					bestVariable = v;
					bestIndexScore = indexScore;
				}
			}

		}

		if (bestVariable == null) {
			for (Variable v : variables) {
				if (!v.isAssigned()) {
					bestVariable = v;
					break;
				}
			}
		}

		return Pair.pair(bestVariable, valueHeuristic.selectIndex(bestVariable));
	}

	public void compute() {
		valueHeuristic.compute();
	}
	
    public String toString() {
    	return "Discriminated " + variableHeuristic + ", " + valueHeuristic ;
    }
}