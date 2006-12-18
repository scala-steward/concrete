package cspfj.util;

public class BooleanArray {
	public final static int[] MASKS;

	static {
		MASKS = new int[Integer.SIZE];
		for (int i = 0; i < MASKS.length; i++) {
			MASKS[i] = 0x1 << (Integer.SIZE - i - 1);
		}
	}

	private BooleanArray() {
		super();
	}

	public static int[] newBooleanArray(final int size, final boolean fill) {
		final int[] array = new int[booleanArraySize(size)];
		initBooleanArray(array, size, fill);

		return array;
	}

	public static void initBooleanArray(final int[] array, final int size,
			final boolean fill) {
		for (int i = array.length; --i >= 0;) {
			array[i] = fill ? 0xFFFFFFFF : 0;
		}
		array[array.length - 1] <<= Integer.SIZE - (size % Integer.SIZE);
	}

	public static int booleanArraySize(final int size) {
		return size / Integer.SIZE + ((size % Integer.SIZE) > 0 ? 1 : 0);
	}

	public static boolean set(final int[] array, final int position,
			final boolean status) {

		int part = array[position / Integer.SIZE];
		final int i = Integer.bitCount(part);

		if (status) {
			part |= MASKS[position % Integer.SIZE];
		} else {
			part &= ~MASKS[position % Integer.SIZE];

		}

		array[position / Integer.SIZE] = part;
		return Integer.bitCount(part) != i;
	}

	public static boolean isTrue(final int[] array, final int position) {
		return (array[position / Integer.SIZE] & MASKS[position % Integer.SIZE]) != 0;
	}

	public static int leftMostCommonIndex(final int[] mask, final int[] domain) {
		for (int i = 0; i < mask.length; i++) {
			final int valid = mask[i] & domain[i];
			// System.err.println(Integer.toBinaryString(mask[i]) + " & "
			// + Integer.toBinaryString(domain[i]) + " = "
			// + Integer.numberOfLeadingZeros(valid));
			if (valid != 0) {
				return Integer.SIZE * i + Integer.numberOfLeadingZeros(valid);
			}

		}
		return -1;
	}

	public static String toString(final int[] array) {
		final StringBuffer sb = new StringBuffer();

		for (int i = 0; i < array.length; i++) {
			sb.append(Integer.toBinaryString(array[i]));
			// sb.append(array[i]) ;
			if (i + 1 < array.length) {
				sb.append('-');
			}
		}
		return sb.toString();
	}

	public static void setSingle(final int[] booleanDomain, final int index) {
		initBooleanArray(booleanDomain, 0, false);
		set(booleanDomain, index, true);

	}

	public static int getPart(final int i) {
		return i / Integer.SIZE;
	}

	//
	// public static void main(final String[] args) {
	// final int[] cb = newBooleanArray(37, true);
	//
	// System.out.println(toString(cb));
	//
	// final int[] mask = newBooleanArray(40, false);
	// set(cb, 7, true);
	// set(mask, 7, true);
	// set(cb, 31, true);
	// System.out.println(leftMostCommonIndex(cb, mask));
	//
	// }

}