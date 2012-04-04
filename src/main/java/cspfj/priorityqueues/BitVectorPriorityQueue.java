package cspfj.priorityqueues;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;

public final class BitVectorPriorityQueue<T extends Identified> extends
        AbstractQueue<T> {

    private final BitSet queue;

    private T[] values;

    private int[] evals;

    private final Key<T> key;

    public BitVectorPriorityQueue(final Key<T> key) {
        this(key, 10);
    }

    @SuppressWarnings("unchecked")
    public BitVectorPriorityQueue(final Key<T> key, final int initSize) {
        this.values = (T[]) new Identified[initSize];
        evals = new int[initSize];
        queue = new BitSet(initSize);
        this.key = key;
    }

    /**
     * Increases the capacity of this instance, if necessary, to ensure that it
     * can hold at least the number of elements specified by the minimum
     * capacity argument.
     * 
     * @param minCapacity
     *            the desired minimum capacity
     */
    private void ensureCapacity(final int minCapacity) {
        int oldCapacity = values.length;

        if (minCapacity > oldCapacity) {
            final int newCapacity = Math.max(minCapacity,
                    (oldCapacity * 3) / 2 + 1);
            // minCapacity is usually close to size, so this is a win:
            values = Arrays.copyOf(values, newCapacity);
            evals = Arrays.copyOf(evals, newCapacity);
        }
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int size() {
        return queue.cardinality();
    }

    @Override
    public boolean offer(final T e) {
        final int id = e.getId();
        ensureCapacity(id + 1);
        values[id] = e;
        evals[id] = key.getKey(e);
        if (queue.get(id)) {
            return false;
        } else {
            queue.set(id);
            return true;
        }
    }

    @Override
    public T peek() {
        return values[min()];
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public T poll() {
        final int min = min();
        queue.set(min, false);
        return values[min];
    }

    private int min() {
        int best = queue.nextSetBit(0);
        int bestKey = key.getKey(values[best]);
        for (int i = queue.nextSetBit(best + 1); i >= 0; i = queue
                .nextSetBit(i + 1)) {
            final int keyValue = evals[i];

            if (keyValue < bestKey) {
                best = i;
                bestKey = keyValue;
            }
        }

        return best;
    }
}
