package org.deuce.optimize.analyses.pre.lcm;

import soot.Body;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;

public class Manipulations {
	private final Body body;

	public Manipulations(Body body) {
		this.body = body;
	}

	public void redirectExitToNewTarget(Unit previousTarget, Unit newTarget,
			Unit exitUnit) {
		if (exitUnit instanceof GotoStmt)
			((GotoStmt) exitUnit).setTarget(newTarget);
		else if (exitUnit instanceof IfStmt) {
			if (((IfStmt) exitUnit).getTarget() == previousTarget)
				((IfStmt) exitUnit).setTarget(newTarget);
			else
				insertGotoAfter(exitUnit, newTarget);
		} else
			insertGotoAfter(exitUnit, newTarget);
	}

	/**
	 * inserts a Jimple<code>Goto</code> to <code> target, directly after
	 * <code>node</code> in the <code>unitChain</code> of the body.<br>
	 * As we use <code>JGoto</code> the chain must contain Jimple-stmts.
	 * 
	 * @param node
	 *            the <code>Goto</code> will be inserted just after this node.
	 * @param target
	 *            is the Unit the <code>goto</code> will jump to.
	 * @return the newly inserted <code>Goto</code>
	 */
	public Unit insertGotoAfter(Unit node, Unit target) {
		Unit newGoto = Jimple.v().newGotoStmt(target);
		body.getUnits().insertAfter(newGoto, node);
		return newGoto;
	}

	public Unit getAnyReturnUnit() {
		for (Unit unit : body.getUnits()) {
			if (unit instanceof ReturnStmt || unit instanceof ReturnVoidStmt)
				return unit;
		}
		throw new RuntimeException("Body has no return statement?");
		// return body.getUnits().getLast();
	}
}
