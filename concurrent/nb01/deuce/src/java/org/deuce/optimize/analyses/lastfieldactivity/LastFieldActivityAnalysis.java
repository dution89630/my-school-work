package org.deuce.optimize.analyses.lastfieldactivity;

import java.util.List;

import org.deuce.optimize.analyses.atomicstarter.AtomicStartersDatabase;
import org.deuce.optimize.analyses.general.StmtAndMethodStorage;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.GraphUtils;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.OptimizerUtils;

import soot.Body;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class LastFieldActivityAnalysis {

	private final Body body;
	private final SootMethod sootMethod;
	private final ExceptionalUnitGraph graph;
	private final LastFieldActivityDatabase lastFieldActivityDatabase;
	private final OptimizerUtils optimizerUtils;

	public LastFieldActivityAnalysis(ExceptionalUnitGraph graph) {
		// this analysis finds all the last field activities.
		// a last field activity (LFA) is a field write or read statement, 
		// which dominates all other field write or read statements, and is not 
		// contained in a loop.
		// the number of LFAs a single method may have can be zero or any 
		// positive number. zero, in case it has no field activities, or
		// those field activities adjacent to exit points in a loop.
		// larger than 1, in case there are multiple exit points, and each
		// adjacent field activity is not contained in a loop.
		this.graph = graph;
		this.body = graph.getBody();
		this.sootMethod = body.getMethod();
		this.lastFieldActivityDatabase = LastFieldActivityDatabase
				.getInstance();
		this.optimizerUtils = new OptimizerUtils();

	}

	public void analyze() {
		Logger.println("LFAA: Analyzing method: " + sootMethod.toString(),
				sootMethod);
		if (AtomicStartersDatabase.getInstance().isMethodAtomicStarter(
				sootMethod)) {
			PatchingChain<Unit> units = body.getUnits();
			for (Unit unit : units) {
				Stmt stmt = (Stmt) unit;
				if (stmt.containsFieldRef() || stmt.containsArrayRef()
						|| stmt.containsInvokeExpr()) {
					if (isStmtLastFieldActivity(stmt)) {
						addToDatabase(sootMethod.getDeclaringClass(),
								sootMethod, stmt);

					}
				}
			}
		}
	}

	private boolean isStmtLastFieldActivity(Stmt stmt) {
		return isStmtMustBeInstrumented(stmt)
				&& !isStmtFollowedByAStmtThatMustBeInstrumented(stmt);
	}

	private boolean isStmtMustBeInstrumented(Stmt stmt) {
		return optimizerUtils.isStmtMustBeInstrumented(stmt, sootMethod);
	}

	private boolean isStmtFollowedByAStmtThatMustBeInstrumented(Stmt stmt) {
		List<Unit> succsRecursively = GraphUtils.getSuccsRecursively(graph,
				stmt);
		for (Unit succ : succsRecursively) {
			Stmt succStmt = (Stmt) succ;
			if (isStmtMustBeInstrumented(succStmt))
				return true;
		}
		// all successors do not have to be instrumented
		return false;
	}

	private void addToDatabase(SootClass sootClass, SootMethod sootMethod,
			Stmt stmt) {
		List<UniqueCodePoint> uniqueCodePoints = UniqueCodePoint
				.generateUniqueCodePointsFor(sootMethod, stmt);
		for (UniqueCodePoint uniqueCodePoint : uniqueCodePoints) {
			lastFieldActivityDatabase.put(uniqueCodePoint);
			StmtAndMethodStorage.put(uniqueCodePoint, sootMethod, stmt);
		}

		Logger.println("LFAA: " + sootMethod + ": " + stmt
				+ " is last field activity.", sootMethod);
	}
}
