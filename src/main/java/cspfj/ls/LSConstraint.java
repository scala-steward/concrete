package cspfj.ls;

import java.util.Map;

import cspfj.constraint.Constraint;
import cspfj.problem.Variable;

public final class LSConstraint {

    private final Constraint constraint;

    private final int[] tuple;

    private final LSVariable[] scope;

    public LSConstraint(Constraint c, Map<Variable, LSVariable> lsVariables) {
        constraint = c;
        scope = new LSVariable[c.getArity()];
        for (int i = c.getArity(); --i >= 0;) {
            scope[i] = lsVariables.get(c.getVariable(i));
        }
        tuple = c.getTuple();
    }

    public boolean check() {
        for (int i = tuple.length; --i >= 0;) {
            tuple[i] = scope[i].getAssignedIndex();
        }
        return constraint.check();
    }

    public boolean checkWith(int variablePosition, int value) {
        for (int i = tuple.length; --i >= 0;) {
            if (i == variablePosition) {
                tuple[i] = value;
            } else {
                tuple[i] = scope[i].getAssignedIndex();
            }
        }
        return constraint.check();
    }

    public int getPosition(Variable variable) {
        return constraint.getPosition(variable);
    }

    public int getPositionInVariable(int variablePosition) {
        return constraint.getPositionInVariable(variablePosition);
    }

    public LSVariable[] getScope() {
        return scope;
    }

    public String toString() {
        return "ls-" + constraint.toString();
    }
}