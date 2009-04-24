package cspfj.constraint;

import java.util.Comparator;

public final class Weight implements Comparator<Constraint> {

	final private boolean reverse;

	public Weight(final boolean reverse) {
		super();
		this.reverse = reverse;
	}

	public int compare(final Constraint arg0, final Constraint arg1) {
		final int compare = Double.compare(arg0.getWeight(), arg1.getWeight());

		if (compare == 0) {
			return arg0.getId() - arg1.getId();
		}

		return reverse ? -compare : compare;
	}

}