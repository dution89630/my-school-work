package org.deuce.optimize.analyses.leakyconstructor;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;

public class LeakyConstructorIntraprocAnalysis {

	private final Body activeBody;
	private final LeakyConstructorInterprocAnalysis leakyConstructorInterprocAnalysis;

	// a leaky constructor is a constructor that exposes the 'this' object.
	// this can conservatively happen in one of several ways:
	// a. assigning 'this' to any variable or field of an object
	// b. throwing 'this' as an exception
	// c. invoking a leaky 'this' or 'super' constructor
	// d. invoking a method and sending 'this' as a parameter

	public LeakyConstructorIntraprocAnalysis(
			LeakyConstructorInterprocAnalysis leakyConstructorInterprocAnalysis,
			Body activeBody) {
		this.leakyConstructorInterprocAnalysis = leakyConstructorInterprocAnalysis;
		this.activeBody = activeBody;
	}

	public LeakyConstructorFlowSet analyze() {
		Local thisLocal = activeBody.getThisLocal();
		for (Unit unit : activeBody.getUnits()) {
			Stmt stmt = (Stmt) unit;
			if (stmt instanceof AssignStmt) {
				Value rightOp = ((AssignStmt) stmt).getRightOp();
				if (rightOp == thisLocal) {
					// v = this, or v.f = this
					return LeakyConstructorFlowSet.leaky();

				}
			}
			if (stmt.containsInvokeExpr()) {
				boolean sendsThisAsAParameter = possiblyLeakyInvocation(
						thisLocal, stmt.getInvokeExpr());
				if (sendsThisAsAParameter) {
					// v = invoke(p1, p2, ..., this, ...)
					// or, v = this.invoke(p1, p2, ...)
					// handle interprocedural method invocation
					LeakyConstructorFlowSet returnValue = leakyConstructorInterprocAnalysis
							.handleMethodInvocation(activeBody.getMethod(),
									null, stmt);
					if (returnValue.equals(LeakyConstructorFlowSet.leaky()))
						return LeakyConstructorFlowSet.leaky();
				}
			}
			if (stmt instanceof ThrowStmt) {
				Value op = ((ThrowStmt) stmt).getOp();
				if (op == thisLocal) {
					// throw this;
					return LeakyConstructorFlowSet.leaky();
				}
			}
		}
		return LeakyConstructorFlowSet.notLeaky();
	}

	private boolean possiblyLeakyInvocation(Local thisLocal,
			InvokeExpr invokeExpr) {
		boolean sendsThisAsAParameter = invokeExpr.getArgs()
				.contains(thisLocal);

		boolean sendsThisAsReceiverParameter = (invokeExpr instanceof InstanceInvokeExpr)
				&& ((InstanceInvokeExpr) invokeExpr).getBase() == thisLocal;
		return sendsThisAsAParameter || sendsThisAsReceiverParameter;
	}
}
