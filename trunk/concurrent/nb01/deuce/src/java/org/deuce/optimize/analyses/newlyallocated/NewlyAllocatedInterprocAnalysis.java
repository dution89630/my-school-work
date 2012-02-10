package org.deuce.optimize.analyses.newlyallocated;

import org.deuce.optimize.analyses.alwaysatomic.NotAlwaysAtomicDatabase;
import org.deuce.optimize.analyses.general.ForwardInterproceduralAnalysis;

import soot.Body;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class NewlyAllocatedInterprocAnalysis extends
		ForwardInterproceduralAnalysis<NewlyAllocatedFlowSet> {

	public NewlyAllocatedInterprocAnalysis() {
		super();
		analyze();
	}

	@Override
	protected NewlyAllocatedFlowSet analyzeMethod(Body activeBody,
			NewlyAllocatedFlowSet parameterValues) {
		ExceptionalUnitGraph graph = new ExceptionalUnitGraph(activeBody);

		LocalMustAliasAnalysis alias = new LocalMustAliasAnalysis(graph);

		NewlyAllocatedIntraprocAnalysis analysis = new NewlyAllocatedIntraprocAnalysis(
				graph, this, parameterValues);
		NewlyAllocatedFlowSet returnValue = analysis.getReturnValue();
		return returnValue;
	}

	@Override
	protected void merge(NewlyAllocatedFlowSet in1, NewlyAllocatedFlowSet in2,
			NewlyAllocatedFlowSet out) {
		NewlyAllocatedFlowSet merged = NewlyAllocatedFlowSet.mergeAndTakeWorse(
				in1, in2);
		merged.copyInto(out);
	}

	@Override
	protected NewlyAllocatedFlowSet newInitialFlow() {
		return new NewlyAllocatedFlowSet();
	}

	@Override
	protected void copy(NewlyAllocatedFlowSet source, NewlyAllocatedFlowSet dest) {
		source.copyInto(dest);
	}

	@Override
	protected NewlyAllocatedFlowSet defaultReturnValue(SootMethod method) {
		return NewlyAllocatedCommon.defaultReturnValue(method);
	}

	@Override
	protected NewlyAllocatedFlowSet defaultParameterValues(SootMethod method) {
		return NewlyAllocatedCommon.defaultParameterValues(method);
	}

	@Override
	protected void populateParameters(SootMethod callerMethod,
			SootMethod calleeMethod, InvokeExpr invokeExpr,
			NewlyAllocatedFlowSet out, NewlyAllocatedFlowSet parameter) {

		// flow the parameters values into the called method(s).
		// if caller method is not AlwaysAtomic, flow TOP for all parameters!
		boolean callerMethodIsAlwaysAtomic = NotAlwaysAtomicDatabase
				.getInstance().isMethodAlwaysAtomic(callerMethod);

		for (int i = 0; i < invokeExpr.getArgCount(); ++i) {
			Value arg = invokeExpr.getArg(i);
			ValueNumber valueNumber = out.getValueNumber(arg);
			NewlyAllocatedLatticeElement element1 = callerMethodIsAlwaysAtomic ? out
					.getLatticeElement(valueNumber)
					: NewlyAllocatedLatticeElement.SuspectedNotNew;

			NewlyAllocatedLatticeElement element2 = parameter
					.getLatticeElement(ValueNumber.get(0 - i));
			NewlyAllocatedLatticeElement merged = NewlyAllocatedLatticeElement
					.takeWorse(element1, element2);
			parameter.putLatticeElement(ValueNumber.get(0 - i), merged);
		}
	}

	@Override
	protected NewlyAllocatedFlowSet handleSkippedMethodInvocation(
			SootMethod callerMethod, NewlyAllocatedFlowSet out,
			InvokeExpr invokeExpr, SootMethod invokedMethod) {
		return new NewlyAllocatedFlowSet();
	}

	@Override
	protected boolean shouldAnalyzeMethod(SootMethod method) {
		return true;
	}

}
