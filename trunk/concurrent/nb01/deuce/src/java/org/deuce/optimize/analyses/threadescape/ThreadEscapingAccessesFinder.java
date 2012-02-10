package org.deuce.optimize.analyses.threadescape;

import java.util.List;

import org.deuce.optimize.analyses.general.StmtAndMethodStorage;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.PointsToHelper;

import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.spark.pag.AllocNode;

public class ThreadEscapingAccessesFinder {
	private final ThreadEscapeDatabase database;
	private SootClass sootClass;
	private SootMethod sootMethod;

	// this analysis finds all accesses to fields:
	// o.f = v
	// v = o.f
	// such that o is a thread-escaping object.

	public ThreadEscapingAccessesFinder() {
		database = ThreadEscapeDatabase.getInstance();
	}

	public void findThreadEscapingAccesses(SootMethod sootMethod) {
		this.sootMethod = sootMethod;
		this.sootClass = sootMethod.getDeclaringClass();

		if (!sootMethod.hasActiveBody())
			return;

		Logger.println("TEA: Analyzing method: " + sootMethod.toString());

		Body activeBody = sootMethod.getActiveBody();
		for (Unit unit : activeBody.getUnits()) {
			Stmt stmt = (Stmt) unit;
			if (stmt instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) stmt;
				Value leftOp = assignStmt.getLeftOp();
				Value rightOp = assignStmt.getRightOp();
				if (stmt.containsArrayRef()) {
					// v = arr[i] OR arr[i] = v
					Local arr = (Local) stmt.getArrayRef().getBase();
					tagIfObjectIsThreadEscaping(stmt, arr);
				}
				if (rightOp instanceof InstanceFieldRef) {
					// v = o.f
					Local o = (Local) ((InstanceFieldRef) rightOp).getBase();
					tagIfObjectIsThreadEscaping(stmt, o);
				}
				if (rightOp instanceof StaticFieldRef) {
					// v = C.f
					tagAsThreadEscaping(stmt);
				}
				if (leftOp instanceof InstanceFieldRef) {
					// o.f = v
					Local o = (Local) ((InstanceFieldRef) leftOp).getBase();
					tagIfObjectIsThreadEscaping(stmt, o);
				}
				if (leftOp instanceof StaticFieldRef) {
					// C.f = v
					tagAsThreadEscaping(stmt);
				}
			}
		}
	}

	private void tagIfObjectIsThreadEscaping(Stmt stmt, Local local) {
		List<AllocNode> nodes = PointsToHelper.getNodes(local);
		// if even one of the nodes escapes the thread, we deem this access
		// to be a thread-escaping access
		for (AllocNode allocNode : nodes) {
			if (database.containsObject(allocNode)) {
				// found a thread-escaping object that is accessed by this
				// statement.
				// tag this statement as thread-escaping and finish.
				tagAsThreadEscaping(stmt);
				return;
			}
		}
		Logger.println("TEA: " + stmt + " is thread-local.");
	}

	private void tagAsThreadEscaping(Stmt stmt) {
		List<UniqueCodePoint> uniqueCodePoints = UniqueCodePoint
				.generateUniqueCodePointsFor(sootMethod, stmt);
		for (UniqueCodePoint uniqueCodePoint : uniqueCodePoints) {
			database.addToThreadEscapingAccesses(uniqueCodePoint);
			StmtAndMethodStorage.put(uniqueCodePoint, sootMethod, stmt);
		}
	}
}