package org.deuce.optimize.analyses.newlocals;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import soot.Unit;
import soot.Value;

public class NewLocalsFlowSet {
	private final Map<Value, ValueNumber> valueToValueNumberMap;
	private final Map<ValueNumber, NewLocalsLatticeElement> valueNumberToLatticeElementMap;

	public NewLocalsFlowSet() {
		valueToValueNumberMap = new LinkedHashMap<Value, ValueNumber>();
		valueNumberToLatticeElementMap = new LinkedHashMap<ValueNumber, NewLocalsLatticeElement>();
	}

	@Override
	public String toString() {
		return valueNumberToLatticeElementMap.toString() + "\n"
				+ valueToValueNumberMap.toString();
	}

	public void copyInto(NewLocalsFlowSet target) {
		target.valueNumberToLatticeElementMap.clear();
		target.valueNumberToLatticeElementMap
				.putAll(this.valueNumberToLatticeElementMap);

		target.valueToValueNumberMap.clear();
		target.valueToValueNumberMap.putAll(this.valueToValueNumberMap);
	}

	public NewLocalsLatticeElement getLatticeElement(ValueNumber value) {
		return valueNumberToLatticeElementMap.get(value);
	}

	public void putLatticeElement(ValueNumber value,
			NewLocalsLatticeElement element) {
		valueNumberToLatticeElementMap.put(value, element);
	}

	public static NewLocalsFlowSet merge(Unit succNode, NewLocalsFlowSet in1,
			NewLocalsFlowSet in2) {
		NewLocalsFlowSet merged = new NewLocalsFlowSet();

		// copy the value number -> lattice element maps		
		Set<ValueNumber> allValueNumbers = new LinkedHashSet<ValueNumber>();
		allValueNumbers.addAll(in1.valueNumberToLatticeElementMap.keySet());
		allValueNumbers.addAll(in2.valueNumberToLatticeElementMap.keySet());

		for (ValueNumber valueNumber : allValueNumbers) {
			NewLocalsLatticeElement element1 = in1.valueNumberToLatticeElementMap
					.get(valueNumber);
			NewLocalsLatticeElement element2 = in2.valueNumberToLatticeElementMap
					.get(valueNumber);
			if (element1 == null && element2 == null)
				assert false;
			else if (element1 == null && element2 != null)
				merged.valueNumberToLatticeElementMap
						.put(valueNumber, element2);
			else if (element1 != null && element2 == null)
				merged.valueNumberToLatticeElementMap
						.put(valueNumber, element1);
			else {
				NewLocalsLatticeElement mergedElement = NewLocalsLatticeElement
						.merge(element1, element2);
				merged.valueNumberToLatticeElementMap.put(valueNumber,
						mergedElement);
			}
		}

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
				ValueNumber mergedValueNumber = ValueNumber.merge(succNode,
						value);
				merged.valueToValueNumberMap.put(value, mergedValueNumber);

				NewLocalsLatticeElement element1 = merged.valueNumberToLatticeElementMap
						.get(valueNumber1);
				NewLocalsLatticeElement element2 = merged.valueNumberToLatticeElementMap
						.get(valueNumber2);
				NewLocalsLatticeElement mergedElement = NewLocalsLatticeElement
						.merge(element1, element2);
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
		NewLocalsFlowSet other = (NewLocalsFlowSet) obj;
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
