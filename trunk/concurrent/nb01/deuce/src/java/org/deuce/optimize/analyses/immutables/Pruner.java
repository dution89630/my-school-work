package org.deuce.optimize.analyses.immutables;

import java.util.List;

import org.deuce.optimize.utils.MethodUtils;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;

public class Pruner {

	private FieldsMutabilityDatabase database;

	/*
	 * Prunes all fields which are either:
	 * (a) written to by methods which are not constructors or static initializers, or
	 * (b) written to by constructors and static initializers, but not to the local (this / same class type) object.
	 */
	public void prune() {
		database = FieldsMutabilityDatabase.getInstance();

		for (SootClass sootClass : Scene.v().getApplicationClasses()) {
			List<SootMethod> methods = sootClass.getMethods();
			for (SootMethod sootMethod : methods) {
				if (sootMethod.hasActiveBody()) {
					Body activeBody = sootMethod.getActiveBody();
					for (Unit unit : activeBody.getUnits()) {
						if (unit instanceof AssignStmt) {
							Value leftOp = ((AssignStmt) unit).getLeftOp();
							if (leftOp instanceof StaticFieldRef) {
								// C.f = v
								handleWriteToStaticField(sootMethod,
										activeBody, (StaticFieldRef) leftOp);
							} else if (leftOp instanceof InstanceFieldRef) {
								// o.f = v    
								handleWriteToInstanceField(sootMethod,
										activeBody, (InstanceFieldRef) leftOp);
							}
						}
					}
				}
			}
		}
	}

	private void handleWriteToStaticField(SootMethod sootMethod,
			Body activeBody, StaticFieldRef leftOp) {
		// handle C.f = v; 
		// if method is not a constructor, we prune this field.
		// if method is a constructor, but C is not the container class of the method, we prune this field.

		boolean methodIsStaticCtor = MethodUtils.methodIsStaticCtor(sootMethod);
		SootField sootField = leftOp.getField();
		SootClass declaringClass = sootField.getDeclaringClass();

		if (!methodIsStaticCtor
				|| (methodIsStaticCtor && declaringClass != sootMethod
						.getDeclaringClass())) {
			database.setFieldAsMutable(sootField);
		}
	}

	private void handleWriteToInstanceField(SootMethod sootMethod,
			Body activeBody, InstanceFieldRef leftOp) {
		// handle o.f = v; 
		// if method is not a constructor, we prune this field.
		// if method is a constructor, but o is not 'this', we prune this field.

		Value base = leftOp.getBase();
		SootField sootField = leftOp.getField();

		boolean methodIsInstanceCtor = MethodUtils
				.methodIsInstanceCtor(sootMethod);
		if (!methodIsInstanceCtor) {
			database.setFieldAsMutable(sootField);
			return;
		}

		Local thisLocal = activeBody.getThisLocal();
		if (base != thisLocal) {
			database.setFieldAsMutable(sootField);
		}
	}

}
