package org.deuce.optimize.analyses.newlocals;

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

public class NewLocalsSceneTransformer extends SceneTransformer {

	private static Transformer instance = new NewLocalsSceneTransformer();

	@Override
	protected void internalTransform(String phaseName, Map options) {
		SootMethod mainMethod = Scene.v().getMainMethod();
		CallGraph callGraph = Scene.v().getCallGraph();

		iterateAllMethods();
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
			return;
		}
		Body activeBody = method.getActiveBody();

		PatchingChain<Unit> units = activeBody.getUnits();

		ExceptionalUnitGraph graph = new ExceptionalUnitGraph(activeBody);

		NewLocalsIntraprocAnalysis a = new NewLocalsIntraprocAnalysis(graph);

		NewLocalsTagger tagger = new NewLocalsTagger();
		tagger.tag(activeBody, a);
	}
}