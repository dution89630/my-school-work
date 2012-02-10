package org.deuce.optimize.analyses.newlyallocated;

import org.deuce.optimize.utils.MethodUtils;

import soot.SootMethod;
import soot.jimple.ParameterRef;

public class NewlyAllocatedCommon {

	public static NewlyAllocatedFlowSet defaultReturnValue(SootMethod method) {
		NewlyAllocatedFlowSet flowSet = new NewlyAllocatedFlowSet();
		flowSet.putLatticeElement(ValueNumber.get(1000),
				NewlyAllocatedLatticeElement.Unknown);
		return flowSet;
	}

	public static NewlyAllocatedFlowSet defaultParameterValues(SootMethod method) {
		NewlyAllocatedFlowSet flowSet = new NewlyAllocatedFlowSet();
		if (MethodUtils.methodIsMain(method) || MethodUtils.methodIsRun(method)) {
			ValueNumber valueNumber = ValueNumber.get(0);
			flowSet.putLatticeElement(valueNumber,
					NewlyAllocatedLatticeElement.SuspectedNotNew);
		}

		return flowSet;
	}

	public static ValueNumber getValueNumberOfCaughtException() {
		return ValueNumber.get(666);
	}

	public static ValueNumber getValueNumberOfReturnValue() {
		return ValueNumber.get(1000);
	}

	public static ValueNumber getValueNumberOfParameterRef(ParameterRef p) {
		return ValueNumber.get(0 - p.getIndex());
	}

	public static ValueNumber getValueNumberOfThisRef() {
		return ValueNumber.get(0);
	}

}
