package org.deuce.optimize.analyses.lastfieldactivity;

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
import soot.toolkits.graph.ExceptionalUnitGraph;

public class LastFieldActivitySceneTransformer extends SceneTransformer {

	private static Transformer instance = new LastFieldActivitySceneTransformer();

	@Override
	protected void internalTransform(String phaseName, Map options) {
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

		LastFieldActivityAnalysis a = new LastFieldActivityAnalysis(graph);
		a.analyze();
	}
}