package org.deuce.optimize.analyses.newlyallocated;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import soot.Value;

public class NewlyAllocatedFlowSet {
	private final Map<Value, ValueNumber> valueToValueNumberMap;
	private final Map<ValueNumber, NewlyAllocatedLatticeElement> valueNumberToLatticeElementMap;

	public NewlyAllocatedFlowSet() {
		valueToValueNumberMap = new LinkedHashMap<Value, ValueNumber>();
		valueNumberToLatticeElementMap = new LinkedHashMap<ValueNumber, NewlyAllocatedLatticeElement>();
	}

	@Override
	public String toString() {
		return valueNumberToLatticeElementMap.toString() + "\n"
				+ valueToValueNumberMap.toString();
	}

	public void copyInto(NewlyAllocatedFlowSet target) {
		target.valueNumberToLatticeElementMap.clear();
		target.valueNumberToLatticeElementMap
				.putAll(this.valueNumberToLatticeElementMap);

		target.valueToValueNumberMap.clear();
		target.valueToValueNumberMap.putAll(this.valueToValueNumberMap);
	}

	public NewlyAllocatedLatticeElement getLatticeElement(ValueNumber value) {
		return valueNumberToLatticeElementMap.get(value);
	}

	public void putLatticeElement(ValueNumber value,
			NewlyAllocatedLatticeElement element) {
		valueNumberToLatticeElementMap.put(value, element);
	}

	public static NewlyAllocatedFlowSet mergeAndTakeWorse(
			NewlyAllocatedFlowSet in1, NewlyAllocatedFlowSet in2) {
		NewlyAllocatedFlowSet merged = new NewlyAllocatedFlowSet();

		// copy the value number -> lattice element maps
		merged.valueNumberToLatticeElementMap
				.putAll(in1.valueNumberToLatticeElementMap);
		merged.valueNumberToLatticeElementMap
				.putAll(in2.valueNumberToLatticeElementMap);

		// copy the values one by one.
		// if there are collisions (= merges), handle them.
		Set<Value> allValuesSet = new LinkedHashSet<Value>();
		allValuesSet.addAll(in1.valueToValueNumberMap.keySet());
		allValuesSet.addAll(in2.valueToValueNumberMap.keySet());

		for (Value value : allValuesSet) {
			ValueNumber valueNumber1 = in1.valueToValueNumberMap.get(value);
			ValueNumber valueNumber2 = in2.valueToValueNumberMap.get(value);
			if (valueNumber1 == null && valueNumber2 == null) {
				assert false;
			} else if (valueNumber1 == null && valueNumber2 != null) {
				merged.valueToValueNumberMap.put(value, valueNumber2);
			} else if (valueNumber1 != null && valueNumber2 == null) {
				merged.valueToValueNumberMap.put(value, valueNumber1);
			} else if (valueNumber1 == valueNumber2) {
				merged.valueToValueNumberMap.put(value, valueNumber1);
			} else {
				// handle collision by creating a new value number
				ValueNumber mergedValueNumber = ValueNumber.merge(valueNumber1,
						valueNumber2);
				merged.valueToValueNumberMap.put(value, mergedValueNumber);

				NewlyAllocatedLatticeElement element1 = merged.valueNumberToLatticeElementMap
						.get(valueNumber1);
				NewlyAllocatedLatticeElement element2 = merged.valueNumberToLatticeElementMap
						.get(valueNumber2);
				NewlyAllocatedLatticeElement mergedElement = NewlyAllocatedLatticeElement
						.takeWorse(element1, element2);
				merged.valueNumberToLatticeElementMap.put(mergedValueNumber,
						mergedElement);
			}
		}

		return merged;
	}

	public void setValueNumber(Value value, int number) {
		valueToValueNumberMap.put(value, ValueNumber.get(number));
	}

	public void setValueNumber(Value value, ValueNumber valueNumber) {
		valueToValueNumberMap.put(value, valueNumber);
	}

	public ValueNumber getValueNumber(Value value) {
		if (!valueToValueNumberMap.containsKey(value)) {
			ValueNumber valueNumber = ValueNumber.getNext();
			valueToValueNumberMap.put(value, valueNumber);
		}
		return valueToValueNumberMap.get(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((valueNumberToLatticeElementMap == null) ? 0
						: valueNumberToLatticeElementMap.hashCode());
		result = prime
				* result
				+ ((valueToValueNumberMap == null) ? 0 : valueToValueNumberMap
						.hashCode());
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
		NewlyAllocatedFlowSet other = (NewlyAllocatedFlowSet) obj;
		if (valueNumberToLatticeElementMap == null) {
			if (other.valueNumberToLatticeElementMap != null) {
				return false;
			}
		} else if (!valueNumberToLatticeElementMap
				.equals(other.valueNumberToLatticeElementMap)) {
			return false;
		}
		if (valueToValueNumberMap == null) {
			if (other.valueToValueNumberMap != null) {
				return false;
			}
		} else if (!valueToValueNumberMap.equals(other.valueToValueNumberMap)) {
			return false;
		}
		return true;
	}

}
