package org.deuce.optimize.analyses.newlocals;

import org.deuce.optimize.utils.MethodUtils;

import soot.SootMethod;
import soot.jimple.ParameterRef;

public class NewLocalsCommon {

	public static NewLocalsFlowSet defaultReturnValue(SootMethod method) {
		NewLocalsFlowSet flowSet = new NewLocalsFlowSet();
		flowSet.putLatticeElement(ValueNumber.get(1000),
				NewLocalsLatticeElement.TopUnknown);
		return flowSet;
	}

	public static NewLocalsFlowSet defaultParameterValues(SootMethod method) {
		NewLocalsFlowSet flowSet = new NewLocalsFlowSet();
		if (MethodUtils.methodIsMain(method) || MethodUtils.methodIsRun(method)) {
			ValueNumber valueNumber = ValueNumber.get(0);
			flowSet.putLatticeElement(valueNumber,
					NewLocalsLatticeElement.BottomSuspectedNotNew);
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
