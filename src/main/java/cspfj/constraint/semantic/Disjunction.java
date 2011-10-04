package cspfj.constraint.semantic;

import java.util.Arrays;

import cspfj.constraint.AbstractConstraint;
import cspfj.filter.RevisionHandler;
import cspfj.problem.BooleanDomain;
import cspfj.problem.UNKNOWN$;
import cspfj.problem.Variable;

public final class Disjunction extends AbstractConstraint {
    private int watch1 = -1;
    private int watch2 = -1;
    private final boolean[] reverses;
    private final BooleanDomain[] domains;
    private final float priority;

    public Disjunction(final Variable... disj) {
        this(disj, new boolean[disj.length]);
    }

    public Disjunction(final Variable[] disj, final boolean[] reverses) {
        super(disj);
        domains = new BooleanDomain[disj.length];
        for (int i = disj.length; --i >= 0;) {
            if (!(disj[i].getDomain() instanceof BooleanDomain)) {
                throw new IllegalArgumentException(
                        "Only boolean domain are allowed");
            }
            final BooleanDomain domain = (BooleanDomain) disj[i].getDomain();
            if (domain.size() < 2) {
                throw new IllegalArgumentException(
                        "Assigning constants to disjunctions is not allowed");
            }
            domains[i] = domain;
        }
        if (reverses == null) {
            this.reverses = new boolean[getArity()];
        } else if (reverses.length != getArity()) {
            throw new IllegalArgumentException(
                    "reverses must cover all variables");
        } else {
            this.reverses = reverses;
        }
        watch1 = seekWatch(-1);
        if (watch1 < 0) {
            throw new IllegalStateException("Unexpected inconsistency");
        }
        watch2 = seekWatch(watch1);
        if (watch2 < 0) {
            setTrue(watch1);
            watch2 = watch1;
        }
        priority = (float) (Math.log(getArity()) / Math.log(2));
    }

    @Override
    public boolean check() {
        for (int i = getArity(); --i >= 0;) {
            if (reverses[i] ^ tuple[i] == 1) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "\\/" + Arrays.toString(getScope());
    }

    @Override
    public boolean revise(final RevisionHandler revisator, final int reviseCount) {
        if (isFalse(watch1)) {
            final int newWatch = seekWatch(watch2);
            if (newWatch < 0) {
                if (!canBeTrue(watch2)) {
                    return false;
                }
                if (setTrue(watch2)) {
                    revisator.revised(this, getVariable(watch2));
                    return true;
                }
            } else {
                watch1 = newWatch;
            }
        }
        if (isFalse(watch2)) {
            final int newWatch = seekWatch(watch1);
            if (newWatch >= 0) {
                watch2 = newWatch;
            } else if (setTrue(watch1)) {
                revisator.revised(this, getVariable(watch1));
            }

        }
        return true;
    }

    private boolean isFalse(final int position) {
        if (reverses[position]) {
            return domains[position].isTrue();
        }
        return domains[position].isFalse();
    }

    private boolean setTrue(final int position) {
        final BooleanDomain dom = domains[position];
        if (!dom.isUnknown()) {
            return false;
        }
        if (reverses[position]) {
            dom.setFalse();
        } else {
            dom.setTrue();
        }
        return true;
    }

    private boolean canBeTrue(final int position) {
        return domains[position].canBe(!reverses[position]);
    }

    private int seekWatch(final int excluding) {
        for (int i = 0; i < getArity(); i++) {
            if (i != excluding && canBeTrue(i)) {
                return i;
            }
        }
        return -1;
    }

    public float getEvaluation() {
        return priority;
    }

}
