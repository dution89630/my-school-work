package org.deuce.optimize.analyses.threadescape;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deuce.optimize.utils.PointsToHelper;

import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transformer;
import soot.jimple.spark.pag.AllocNode;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ThreadEscapeSceneTransformer extends SceneTransformer {

	private static Transformer instance = new ThreadEscapeSceneTransformer();
	private ThreadEscapeDatabase database;

	@Override
	protected void internalTransform(String phaseName, Map options) {
		database = ThreadEscapeDatabase.getInstance();
		findAllThreadEscapingObjects();
		tagAllThreadEscapingAccesses();
	}

	private void tagAllThreadEscapingAccesses() {
		ThreadEscapingAccessesFinder finder = new ThreadEscapingAccessesFinder();
		// we must consider all application classes, rather than the atomic methods.
		// because an object may become thread-shared outside of any atomic block.
		for (SootClass sootClass : Scene.v().getApplicationClasses()) {
			List<SootMethod> methods = sootClass.getMethods();
			for (SootMethod sootMethod : methods) {
				finder.findThreadEscapingAccesses(sootMethod);
			}
		}
	}

	private void findAllThreadEscapingObjects() {

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

		List<AllocNode> escapingObjects = runIntraprocAnalysis(activeBody);
		addAllObjectsAccessibleFromEscapingObjectsToDatabase(escapingObjects);
	}

	private List<AllocNode> runIntraprocAnalysis(Body activeBody) {
		ExceptionalUnitGraph graph = new ExceptionalUnitGraph(activeBody);
		ThreadEscapingObjectsAnalysis tea = new ThreadEscapingObjectsAnalysis(
				graph);
		List<AllocNode> allEscapingObjects = tea.getAllEscapingObjects();
		return allEscapingObjects;
	}

	private void addAllObjectsAccessibleFromEscapingObjectsToDatabase(
			List<AllocNode> escapingObjects) {
		if (escapingObjects.isEmpty())
			return;

		List<AllocNode> allAccessibleNodes = PointsToHelper
				.getAllAccessibleNodes(escapingObjects);
		for (AllocNode allocNode : allAccessibleNodes) {
			// add to database				
			database.addToThreadEscapingObjects(allocNode);
		}
	}

	public static Transformer v() {
		return instance;
	}

}
