package org.deuce.optimize.analyses.immutables;

import java.util.LinkedHashSet;
import java.util.List;

import org.deuce.optimize.analyses.general.StmtAndMethodStorage;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.Logger;

import soot.Body;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;

public class ImmutableFieldsAccessesTagger {
	private final ImmutableFieldsAccessesDatabase immutableAccessesDatabase;
	private final FieldsMutabilityDatabase fieldsMutabilityDatabase;

	public ImmutableFieldsAccessesTagger() {
		immutableAccessesDatabase = ImmutableFieldsAccessesDatabase
				.getInstance();
		fieldsMutabilityDatabase = FieldsMutabilityDatabase.getInstance();
	}

	public void tag() {
		for (SootClass sootClass : Scene.v().getApplicationClasses()) {
			List<SootMethod> methods = sootClass.getMethods();
			for (SootMethod sootMethod : methods) {
				if (sootMethod.hasActiveBody()) {
					Body activeBody = sootMethod.getActiveBody();
					PatchingChain<Unit> units = activeBody.getUnits();
					for (Unit unit : units) {
						Stmt stmt = (Stmt) unit;
						if (stmt.containsFieldRef()) {
							FieldRef fieldRef = stmt.getFieldRef();

							List<SootField> allImmutableFieldsList = fieldsMutabilityDatabase
									.getAllImmutableFields();
							LinkedHashSet<SootField> allMutableFields = new LinkedHashSet<SootField>(
									allImmutableFieldsList);

							if (fieldRef instanceof InstanceFieldRef) {
								InstanceFieldRef instanceFieldRef = (InstanceFieldRef) fieldRef;
								SootField sootField = instanceFieldRef
										.getField();
								if (allMutableFields.contains(sootField)) {
									addToDatabase(activeBody.getMethod()
											.getDeclaringClass(), activeBody
											.getMethod(), stmt);
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
			immutableAccessesDatabase.put(uniqueCodePoint);
			StmtAndMethodStorage.put(uniqueCodePoint, sootMethod, stmt);
		}

		Logger.println("IFT: " + sootMethod + ": " + stmt
				+ " involves a immutable field and can be optimized.",
				sootMethod);
	}

}
