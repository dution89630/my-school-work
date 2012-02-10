package org.deuce.optimize.analyses.alwaysatomic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.MethodUtils;

import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class AlwaysAtomicSceneTransformer extends SceneTransformer {

	private static Transformer instance = new AlwaysAtomicSceneTransformer();
	private CallGraph callGraph;
	private NotAlwaysAtomicDatabase database;

	@Override
	protected void internalTransform(String phaseName, Map options) {
		// use the call graph to find all methods which are always atomic.
		// always atomic methods are methods which are either:
		// a. adorned with @Atomic, or
		// b. accessible only through callers which are always atomic.

		// algorithm: start with main() and run() methods.
		// traverse the paths of non-atomic method invocations. all such methods are marked
		// as not-always-atomic.
		// the rest of the methods are always-atomic.

		callGraph = Scene.v().getCallGraph();
		database = NotAlwaysAtomicDatabase.getInstance();

		List<SootMethod> nonAtomicEntryPoints = MethodUtils
				.findNonAtomicEntryPoints();

		traverseNonAtomicMethods(nonAtomicEntryPoints);

		Logger.println("AA: " + database);
	}

	private void traverseNonAtomicMethods(List<SootMethod> nonAtomicEntryPoints) {

		for (SootMethod sootMethod : nonAtomicEntryPoints) {
			if (database.contains(sootMethod))
				continue;
			database.add(sootMethod);

			List<SootMethod> invokedTargets = new ArrayList<SootMethod>();
			Iterator<Edge> edgesOutOf = callGraph.edgesOutOf(sootMethod);
			while (edgesOutOf.hasNext()) {
				Edge edge = edgesOutOf.next();
				if (edge.isExplicit()) {
					SootMethod target = edge.getTgt().method();
					if (!MethodUtils.methodIsAtomic(target)) {
						invokedTargets.add(target);
					}
				}
			}
			traverseNonAtomicMethods(invokedTargets);
		}
	}

	public static Transformer v() {
		return instance;
	}

}
