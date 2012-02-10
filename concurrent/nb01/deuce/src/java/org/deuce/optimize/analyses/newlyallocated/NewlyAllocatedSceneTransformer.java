package org.deuce.optimize.analyses.newlyallocated;

import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.PatchingChain;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transformer;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class NewlyAllocatedSceneTransformer extends SceneTransformer {

	private static Transformer instance = new NewlyAllocatedSceneTransformer();

	@Override
	protected void internalTransform(String phaseName, Map options) {
		SootMethod mainMethod = Scene.v().getMainMethod();
		CallGraph callGraph = Scene.v().getCallGraph();

		// TODO: REPLACE WITH INTERPROC ANALYSIS
		//iterateAllMethods();
		NewlyAllocatedInterprocAnalysis a = new NewlyAllocatedInterprocAnalysis();

	}

	public static Transformer v() {
		return instance;
	}

	private void iterateAllMethods() {

		// Find methods
		Iterator<SootClass> getClassesIt = Scene.v().getApplicationClasses()
				.iterator();
		while (getClassesIt.hasNext()) {
			SootClass appClass = getClassesIt.next();
			Iterator<SootMethod> getMethodsIt = appClass.getMethods()
					.iterator();
			while (getMethodsIt.hasNext()) {
				SootMethod method = getMethodsIt.next();
				analyzeMethod(method);
			}
		}
	}

	private void analyzeMethod(SootMethod method) {
		if (!method.hasActiveBody()) {
			// System.out.println("No body found for " + method.toString());
			return;
		} else {
			// System.out.println("Body was found for " + method.toString());
		}
		Body activeBody = method.getActiveBody();

		PatchingChain<Unit> units = activeBody.getUnits();

		//ValueNumberingAnalysis a = new ValueNumberingAnalysis(
		//	new ExceptionalUnitGraph(activeBody));
		ExceptionalUnitGraph graph = new ExceptionalUnitGraph(activeBody);

		NewlyAllocatedIntraprocAnalysis a = new NewlyAllocatedIntraprocAnalysis(
				graph, null, null);

	}
}