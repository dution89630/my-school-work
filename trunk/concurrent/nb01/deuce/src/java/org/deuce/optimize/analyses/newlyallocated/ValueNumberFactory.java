package org.deuce.optimize.analyses.newlyallocated;
//package optimize.newlyallocated;
//
//import java.util.LinkedHashMap;
//
//import soot.Local;
//import soot.Value;
//import soot.jimple.ParameterRef;
//
//public class ValueNumberFactory {
//
//	private final LinkedHashMap<Integer, ValueNumber> numberToValueNumberMap;
//	private int nextNumber;
//
//	public ValueNumberFactory() {
//		numberToValueNumberMap = new LinkedHashMap<Integer, ValueNumber>();
//		valueToValueNumberMap = new LinkedHashMap<Value, ValueNumber>();
//		nextNumber = 1;
//	}
//
//	public ValueNumber getValueNumber(Value value) {
//		/*if (value instanceof ThisRef) {
//			return fetchValueNumber(thisRefNumber());
//		}
//		if (value instanceof ParameterRef) {
//			return fetchValueNumber(parameterRefNumber((ParameterRef) value));
//		}
//		if (value instanceof AnyNewExpr) {
//
//		}*/
//
//		if (!valueToValueNumberMap.containsKey(value)) {
//			setValueNumber(value, fetchValueNumber(nextNumber++));
//		}
//		return valueToValueNumberMap.getLatticeElement(value);
//
//		// handle local, field refs and new expressions
//
//		//		throw new RuntimeException("Cannot get Value Number for value: "
//		//				+ value.toString());
//	}
//
//	private int thisRefNumber() {
//		//unique number for ThisRef (must be <1)
//		return 0;
//	}
//
//	private int parameterRefNumber(ParameterRef r) {
//		//unique number for ParameterRef[i] (must be <0)
//		return 0 - r.getIndex();
//	}
//
//	private ValueNumber fetchValueNumber(int i) {
//		if (numberToValueNumberMap.containsKey(i))
//			return numberToValueNumberMap.get(i);
//		ValueNumber valueNumber = new ValueNumber(i);
//		numberToValueNumberMap.put(i, valueNumber);
//		return numberToValueNumberMap.get(i);
//	}
//
//	public void setValueNumber(Value leftOp, ValueNumber valueNumber) {
//		valueToValueNumberMap.putLatticleElement(leftOp, valueNumber);
//	}
//
//	public void setValueNumber(Local left, int i) {
//		setValueNumber(left, fetchValueNumber(i));
//	}
//
//}
