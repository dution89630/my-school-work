package org.deuce.optimize.analyses.newlocals;

import java.util.LinkedHashMap;
import java.util.Map;

import soot.Unit;
import soot.Value;

public class ValueNumber {

	private static Map<Unit, Map<Value, ValueNumber>> mergePointToValueToNumber;
	private static Map<Integer, ValueNumber> numberToValueNumberMap;

	static {
		reset();
	}

	public static void reset() {
		numberToValueNumberMap = new LinkedHashMap<Integer, ValueNumber>();
		mergePointToValueToNumber = new LinkedHashMap<Unit, Map<Value, ValueNumber>>();
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

	public static ValueNumber merge(Unit succUnit, Value value) {

		Map<Value, ValueNumber> valueToNumber = mergePointToValueToNumber
				.get(succUnit);
		if (valueToNumber == null) {
			valueToNumber = new LinkedHashMap<Value, ValueNumber>();
			mergePointToValueToNumber.put(succUnit, valueToNumber);
		}

		if (!valueToNumber.containsKey(value)) {
			valueToNumber.put(value, getNext());
		}
		return valueToNumber.get(value);
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
