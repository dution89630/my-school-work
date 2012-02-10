package org.deuce.optimize.analyses.newlyallocated;

import java.util.LinkedHashMap;
import java.util.Map;

import soot.toolkits.scalar.IdentityPair;

public class ValueNumber {

	private static LinkedHashMap<IdentityPair<ValueNumber, ValueNumber>, ValueNumber> pairToMergedValueNumberMap;
	private static Map<Integer, ValueNumber> numberToValueNumberMap;

	static {
		reset();
	}

	public static void reset() {
		numberToValueNumberMap = new LinkedHashMap<Integer, ValueNumber>();
		pairToMergedValueNumberMap = new LinkedHashMap<IdentityPair<ValueNumber, ValueNumber>, ValueNumber>();
		nextNumber = 1;
	}

	private static int nextNumber;
	private final int number;

	private ValueNumber(int number) {
		this.number = number;
	}

	public static ValueNumber get(int number) {
		if (numberToValueNumberMap.containsKey(number))
			return numberToValueNumberMap.get(number);
		ValueNumber valueNumber = new ValueNumber(number);
		numberToValueNumberMap.put(number, valueNumber);
		return numberToValueNumberMap.get(number);
	}

	public static ValueNumber getNext() {
		return get(nextNumber++);
	}

	@Override
	public String toString() {
		return Integer.toString(number);
	}

	public static ValueNumber merge(ValueNumber valueNumber1,
			ValueNumber valueNumber2) {
		IdentityPair<ValueNumber, ValueNumber> pair = new IdentityPair<ValueNumber, ValueNumber>(
				valueNumber1, valueNumber2);
		if (!pairToMergedValueNumberMap.containsKey(pair)) {
			pairToMergedValueNumberMap.put(pair, getNext());
		} else {
			assert true;
		}
		return pairToMergedValueNumberMap.get(pair);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ValueNumber other = (ValueNumber) obj;
		if (number != other.number) {
			return false;
		}
		return true;
	}

}
