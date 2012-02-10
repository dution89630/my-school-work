package org.deuce.optimize.analyses.immutables;

import java.util.LinkedHashSet;
import java.util.List;

import org.deuce.optimize.utils.MethodUtils;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.StaticFieldRef;

public class Pruner2 {

	private FieldsMutabilityDatabase database;
	private LinkedHashSet<SootField> initializedInInstanceCtor;
	private LinkedHashSet<SootField> initializedInStaticCtor;

	/*
	 * Prunes all static fields which are written to both by a ctor and by a static initializer.
	 */
	public void prune() {
		database = FieldsMutabilityDatabase.getInstance();
		initializedInInstanceCtor = new LinkedHashSet<SootField>();
		initializedInStaticCtor = new LinkedHashSet<SootField>();

		buildMaps();

		pruneIntersections();
	}

	private void pruneIntersections() {
		initializedInInstanceCtor.retainAll(initializedInStaticCtor);
		for (SootField sootField : initializedInInstanceCtor) {
			// sootField is initialized both in a static initializer and in a instance constructor.
			// it is therefore deemed mutable.
			database.setFieldAsMutable(sootField);
		}
	}

	private void buildMaps() {
		for (SootClass sootClass : Scene.v().getApplicationClasses()) {
			List<SootMethod> methods = sootClass.getMethods();
			for (SootMethod sootMethod : methods) {
				if (MethodUtils.methodIsCtor(sootMethod)) {
					if (sootMethod.hasActiveBody()) {
						Body activeBody = sootMethod.getActiveBody();
						for (Unit unit : activeBody.getUnits()) {
							if (unit instanceof AssignStmt) {
								Value leftOp = ((AssignStmt) unit).getLeftOp();
								if (leftOp instanceof StaticFieldRef) {
									// C.f = v
									SootField sootField = ((StaticFieldRef) leftOp)
											.getField();
									if (MethodUtils
											.methodIsStaticCtor(sootMethod))
										initializedInStaticCtor.add(sootField);
									else
										initializedInInstanceCtor
												.add(sootField);
								}
							}
						}
					}
				}
			}
		}
	}

}
