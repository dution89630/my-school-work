package org.deuce.optimize.analyses.puremethod;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.MethodUtils;
import org.deuce.optimize.utils.OptimizerUtils;

import soot.Body;
import soot.PatchingChain;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transformer;
import soot.Unit;
import soot.jimple.Stmt;

public class PureMethodSceneTransformer extends SceneTransformer {

	private static Transformer instance = new PureMethodSceneTransformer();
	private PureMethodsDatabase database;
	private final OptimizerUtils optimizerUtils;

	public PureMethodSceneTransformer() {
		this.optimizerUtils = new OptimizerUtils();
	}

	@Override
	protected void internalTransform(String phaseName, Map options) {
		// use the call graph to find all pure methods.
		// a method is pure if it:
		// (a) doesn't access any field or array
		// (b) doesn't invoke any non-pure method

		// algorithm: start with main() and run() methods.
		// first, find all directly pure methods -- satisfy (a).
		// then, verify that they satisfy (b) as well.

		database = PureMethodsDatabase.getInstance();

		List<SootMethod> entryPoints = MethodUtils.findApplicationEntryPoints();
		Set<SootMethod> reachables = MethodUtils
				.findTransitiveCalleesOf(entryPoints);

		discoverDirectlyPureMethods(reachables);
		discoverPureMethodsThatCallImpureMethods();

		Logger.println("PM: " + database);
	}

	private void discoverDirectlyPureMethods(Set<SootMethod> reachables) {
		for (SootMethod sootMethod : reachables) {
			if (areMethodsFieldAccessesPure(sootMethod))
				database.add(sootMethod);
		}
	}

	private boolean areMethodsFieldAccessesPure(SootMethod sootMethod) {
		if (!sootMethod.hasActiveBody()) {
			return false;
		}
		Body activeBody = sootMethod.getActiveBody();
		PatchingChain<Unit> units = activeBody.getUnits();
		for (Unit unit : units) {
			Stmt stmt = (Stmt) unit;
			if (stmt.containsFieldRef() || stmt.containsArrayRef()) {
				if (optimizerUtils
						.mustFieldStmtBeInstrumented(stmt, sootMethod)) {
					return false;
				}
			}
		}
		return true;
	}

	private void discoverPureMethodsThatCallImpureMethods() {
		Set<SootMethod> impures = new LinkedHashSet<SootMethod>();

		while (true) {
			for (SootMethod sootMethod : database.getPureMethods()) {
				List<SootMethod> callees = MethodUtils.getCalleesOf(sootMethod);
				for (SootMethod callee : callees) {
					if (!database.isMethodPure(callee)) {
						// a pure calling impure... therefore it is not really pure.
						impures.add(sootMethod);
					}
				}
			}
			for (SootMethod impure : impures) {
				database.remove(impure);
			}
			if (impures.size() == 0)
				return;
			impures.clear();
		}
	}

	public static Transformer v() {
		return instance;
	}

}
