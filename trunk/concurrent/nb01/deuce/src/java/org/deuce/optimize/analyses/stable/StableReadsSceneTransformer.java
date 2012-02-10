package org.deuce.optimize.analyses.stable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.deuce.optimize.analyses.general.StmtAndMethodStorage;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.Logger;

import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transformer;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BreakpointStmt;
import soot.jimple.FieldRef;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;

public class StableReadsSceneTransformer extends SceneTransformer {

	private static Transformer instance = new StableReadsSceneTransformer();
	private final StableReadsDatabase database = StableReadsDatabase
			.getInstance();

	@Override
	protected void internalTransform(String phaseName, Map options) {
		// a read is stable if it cannot cause an endless loop to occur, even
		// if an inconsistent value is read.
		// stable reads can skip online validations. They will be validated
		// later, in the commit() method.

		// algorithm: find all methods which:
		// 1. don't invoke other methods, and
		// 2. don't have any conditions, and
		// 3. write to at least one array or field.

		// tag all the reads found in such methods as stable reads.

		findMethodsWithStableReads();
		tagStableReads();
	}

	private void tagStableReads() {
		LinkedHashSet<SootMethod> allMethods = database.getAllMethods();
		for (SootMethod sootMethod : allMethods) {
			Body activeBody = sootMethod.getActiveBody();
			for (Unit unit : activeBody.getUnits()) {
				if (unit instanceof AssignStmt) {
					Value rightOp = ((AssignStmt) unit).getRightOp();
					if (rightOp instanceof FieldRef
							|| rightOp instanceof ArrayRef) {
						tagStmt(sootMethod, (Stmt) unit);
					}
				}
			}
		}
	}

	private void tagStmt(SootMethod sootMethod, Stmt stmt) {
		List<UniqueCodePoint> uniqueCodePoints = UniqueCodePoint
				.generateUniqueCodePointsFor(sootMethod, stmt);
		for (UniqueCodePoint uniqueCodePoint : uniqueCodePoints) {
			database.put(uniqueCodePoint);
			StmtAndMethodStorage.put(uniqueCodePoint, sootMethod, stmt);
		}
		if (sootMethod.getDeclaringClass().isApplicationClass()) {
			Logger.println("SRT: " + sootMethod + ": " + stmt
					+ " is a stable read.", sootMethod);
		}
	}

	private void findMethodsWithStableReads() {
		for (SootClass sootClass : Scene.v().getApplicationClasses()) {
			List<SootMethod> methods = sootClass.getMethods();
			for (SootMethod sootMethod : methods) {
				if (sootMethod.hasActiveBody()) {
					Body activeBody = sootMethod.getActiveBody();
					boolean foundWrite = evaluateMethod(activeBody);
					if (foundWrite) {
						database.put(sootMethod);
					}
				}
			}
		}
	}

	private boolean evaluateMethod(Body activeBody) {
		boolean foundWrite = false;
		for (Unit unit : activeBody.getUnits()) {
			if (unit instanceof IfStmt || unit instanceof GotoStmt
					|| unit instanceof LookupSwitchStmt
					|| unit instanceof TableSwitchStmt
					|| unit instanceof BreakpointStmt
					|| unit instanceof InvokeStmt) {
				return false;
			}
			if (((Stmt)unit).containsInvokeExpr())
				return false;
			if (unit instanceof AssignStmt) {				
				Value leftOp = ((AssignStmt) unit).getLeftOp();
				if (leftOp instanceof FieldRef
						|| leftOp instanceof ArrayRef) {
					foundWrite = true;
				}
			}
		}
		return foundWrite;
	}

	public static Transformer v() {
		return instance;
	}

}
