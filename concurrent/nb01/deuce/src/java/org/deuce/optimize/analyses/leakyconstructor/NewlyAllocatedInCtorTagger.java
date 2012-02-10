package org.deuce.optimize.analyses.leakyconstructor;

import java.util.List;

import org.deuce.optimize.analyses.general.StmtAndMethodStorage;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.MethodUtils;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;

public class NewlyAllocatedInCtorTagger {

	private final LeakyConstructorsDatabase leakyDatabase;
	private final NewlyAllocatedInCtorDatabase newlyAllocatedInCtorDatabase;

	public NewlyAllocatedInCtorTagger() {
		newlyAllocatedInCtorDatabase = NewlyAllocatedInCtorDatabase
				.getInstance();
		leakyDatabase = LeakyConstructorsDatabase.getInstance();
	}

	public void tag() {
		for (SootClass sootClass : Scene.v().getApplicationClasses()) {
			List<SootMethod> methods = sootClass.getMethods();
			for (SootMethod sootMethod : methods) {
				if (MethodUtils.methodIsInstanceCtor(sootMethod)) {
					if (!leakyDatabase.isConstructorLeaky(sootMethod)) {
						if (sootMethod.hasActiveBody()) {
							Body activeBody = sootMethod.getActiveBody();
							Local thisLocal = activeBody.getThisLocal();
							for (Unit unit : activeBody.getUnits()) {
								if (unit instanceof AssignStmt) {
									AssignStmt assignStmt = (AssignStmt) unit;
									if (assignStmt.containsFieldRef()) {
										FieldRef fieldRef = assignStmt
												.getFieldRef();
										if (fieldRef instanceof InstanceFieldRef) {
											Value base = ((InstanceFieldRef) fieldRef)
													.getBase();
											if (base == thisLocal) {
												// found either:
												// this.f = x; 
												// OR 
												// x = this.f;
												// which can be optimized.
												addToDatabase(sootClass,
														sootMethod, assignStmt);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void addToDatabase(SootClass sootClass, SootMethod sootMethod,
			Stmt stmt) {
		List<UniqueCodePoint> uniqueCodePoints = UniqueCodePoint
				.generateUniqueCodePointsFor(sootMethod, stmt);
		for (UniqueCodePoint uniqueCodePoint : uniqueCodePoints) {
			newlyAllocatedInCtorDatabase.add(uniqueCodePoint);
			StmtAndMethodStorage.put(uniqueCodePoint, sootMethod, stmt);
		}

		Logger.println("NAIC: " + sootMethod + ": " + stmt
				+ " can be optimized.", sootMethod);
	}
}