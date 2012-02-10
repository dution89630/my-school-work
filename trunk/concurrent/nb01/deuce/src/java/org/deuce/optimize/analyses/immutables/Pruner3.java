package org.deuce.optimize.analyses.immutables;

import java.util.List;

import org.deuce.optimize.analyses.leakyconstructor.LeakyConstructorsDatabase;
import org.deuce.optimize.utils.MethodUtils;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;

public class Pruner3 {

	/*
	 * Find all leaky constructors.
	 * All fields of those objects are then considered mutable.
	 * A leaky constructor is one which hands the "this" object to a method
	 * invocation or to any field of any object.
	 * 
	 * What's the problem with leaky constructors?
	 * The problem is that they allow for otherwise immutable fields, to appear to 
	 * change their value over time. For example:
	 * 
	 * Dummy() { this.x = 5; invoke(this); this.x = 6; }
	 * 
	 * x visibly appears to change its value from 5 to 6.
	 * 
	 * With non-leaky constructors, a field's value will be set without any change. 
	 */
	
	public void prune() {
		LeakyConstructorsDatabase leakyConstructorsDatabase = LeakyConstructorsDatabase.getInstance();
		for (SootClass sootClass : Scene.v().getApplicationClasses()) {
			if (leakyConstructorsDatabase.hasLeakyConstructor(sootClass)) {
				setClassFieldsAsMutable(sootClass);
			}
		}
	}
	
	private void setClassFieldsAsMutable(SootClass sootClass) {
		FieldsMutabilityDatabase.getInstance().setAllFieldsInClassAsMutable(sootClass);
	}
}