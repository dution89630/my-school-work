package org.deuce.optimize.analyses.atomicstarter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.MethodUtils;

import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transformer;
import soot.jimple.toolkits.callgraph.CallGraph;

public class AtomicStarterSceneTransformer extends SceneTransformer {

	private static Transformer instance = new AtomicStarterSceneTransformer();
	private CallGraph callGraph;
	private AtomicStartersDatabase database;

	public static Transformer v() {
		return instance;
	}

	@Override
	protected void internalTransform(String phaseName, Map options) {
		// use the call graph to find all methods which are atomic starters.
		// atomic starter methods are methods which are both:
		// a. adorned with @Atomic, and
		// b. not accessible directly or indirectly from an atomic starter method.

		// algorithm: start with all atomic methods.
		// traverse all method invocations. all such methods are marked as not-atomic-starters.
		// the rest of the atomic methods are atomic starters.

		callGraph = Scene.v().getCallGraph();
		database = AtomicStartersDatabase.getInstance();

		List<SootMethod> atomicMethods = MethodUtils
				.findApplicationAtomicMethods();
		for (SootMethod sootMethod : atomicMethods) {
			database.put(sootMethod,
					AtomicStartersElement.AtomicAndNotCalledFromAtomic);
		}

		Set<SootMethod> transitiveCallees = findTransitiveCalleesOf(atomicMethods);
		for (SootMethod sootMethod : transitiveCallees) {
			if (MethodUtils.methodIsAtomic(sootMethod)) {
				database.put(sootMethod,
						AtomicStartersElement.AtomicAndCalledFromAtomic);
			} else {
				database.put(sootMethod,
						AtomicStartersElement.CalledFromAtomicButNotAtomic);
			}
		}

		Logger.println("AS: " + database);
	}

	private Set<SootMethod> findTransitiveCalleesOf(
			List<SootMethod> atomicMethods) {
		Set<SootMethod> visitedMethods = new LinkedHashSet<SootMethod>();
		for (SootMethod atomicMethod : atomicMethods) {
			Set<SootMethod> transitiveCallees = MethodUtils
					.findTransitiveCalleesOf(atomicMethod);
			visitedMethods.addAll(transitiveCallees);
		}
		return visitedMethods;
	}

}
