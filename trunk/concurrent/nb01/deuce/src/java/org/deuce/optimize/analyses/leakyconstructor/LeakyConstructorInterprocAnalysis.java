package org.deuce.optimize.analyses.leakyconstructor;

import java.util.List;
import java.util.Set;

import org.deuce.optimize.analyses.general.ForwardInterproceduralAnalysis;
import org.deuce.optimize.utils.MethodUtils;

import soot.Body;
import soot.SootMethod;
import soot.jimple.InvokeExpr;

public class LeakyConstructorInterprocAnalysis extends
		ForwardInterproceduralAnalysis<LeakyConstructorFlowSet> {

	public LeakyConstructorInterprocAnalysis() {
		super();
		analyze();
	}

	@Override
	protected LeakyConstructorFlowSet analyzeMethod(Body activeBody,
			LeakyConstructorFlowSet parameterValues) {
		LeakyConstructorIntraprocAnalysis analysis = new LeakyConstructorIntraprocAnalysis(
				this, activeBody);
		LeakyConstructorFlowSet result = analysis.analyze();
		if (result.equals(LeakyConstructorFlowSet.leaky())) {
			LeakyConstructorsDatabase database = LeakyConstructorsDatabase
					.getInstance();
			database.putLeakyConstructor(activeBody.getMethod());
		}
		return result;
	}

	@Override
	protected void copy(LeakyConstructorFlowSet source,
			LeakyConstructorFlowSet dest) {
		source.copy(dest);
	}

	@Override
	protected LeakyConstructorFlowSet defaultParameterValues(SootMethod method) {
		return LeakyConstructorFlowSet.unknown();
	}

	@Override
	protected LeakyConstructorFlowSet defaultReturnValue(SootMethod method) {
		return LeakyConstructorFlowSet.unknown();
	}

	@Override
	protected LeakyConstructorFlowSet handleSkippedMethodInvocation(
			SootMethod callerMethod, LeakyConstructorFlowSet out,
			InvokeExpr invokeExpr, SootMethod invokedMethod) {
		return LeakyConstructorFlowSet.leaky();
	}

	@Override
	protected void merge(LeakyConstructorFlowSet in1,
			LeakyConstructorFlowSet in2, LeakyConstructorFlowSet out) {
		in1.merge(in2, out);
	}

	@Override
	protected LeakyConstructorFlowSet newInitialFlow() {
		return LeakyConstructorFlowSet.unknown();
	}

	@Override
	protected void populateParameters(SootMethod callerMethod,
			SootMethod calleeMethod, InvokeExpr invokeExpr,
			LeakyConstructorFlowSet out, LeakyConstructorFlowSet parameter) {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean shouldAnalyzeMethod(SootMethod method) {
		return MethodUtils.methodIsInstanceCtor(method);
	}

	@Override
	protected List<SootMethod> getEntryPoints() {
		List<SootMethod> entryPoints = MethodUtils.findApplicationEntryPoints();
		Set<SootMethod> reachables = MethodUtils
				.findTransitiveCalleesOf(entryPoints);
		entryPoints.clear();
		for (SootMethod sootMethod : reachables) {
			if (MethodUtils.methodIsInstanceCtor(sootMethod))
				entryPoints.add(sootMethod);
		}
		return entryPoints;
	}

}