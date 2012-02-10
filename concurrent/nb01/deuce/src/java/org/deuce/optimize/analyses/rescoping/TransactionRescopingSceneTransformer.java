package org.deuce.optimize.analyses.rescoping;

import java.util.Iterator;
import java.util.Map;

import org.deuce.optimize.analyses.rescoping.firstfieldactivity.FirstFieldActivityAnalyzer;
import org.deuce.optimize.analyses.rescoping.lastfieldactivity.CommitPointAnalyzer;
import org.deuce.optimize.analyses.rescoping.lastfieldactivity.CommitPointDatabase;

import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transformer;

public class TransactionRescopingSceneTransformer extends SceneTransformer {

	private static Transformer instance = new TransactionRescopingSceneTransformer();
	private FirstFieldActivityAnalyzer firstFieldActivityAnalyzer;
	private CommitPointAnalyzer commitPointsAnalyzer;

	@Override
	protected void internalTransform(String phaseName, Map options) {
		CommitPointDatabase.getInstance();
		firstFieldActivityAnalyzer = new FirstFieldActivityAnalyzer();
		commitPointsAnalyzer = new CommitPointAnalyzer();
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
				firstFieldActivityAnalyzer.analyzeMethod(method);
				commitPointsAnalyzer.analyzeMethod(method);
			}
		}
	}

}