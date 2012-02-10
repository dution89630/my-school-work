package org.deuce.optimize.analyses.general;

import soot.SootMethod;
import soot.jimple.Stmt;

public class UniqueCodePointsAndStmt {
	/**
	 * @return the uniqueCodePoint
	 */
	public UniqueCodePoint getUniqueCodePoint() {
		return uniqueCodePoint;
	}

	/**
	 * @return the stmt
	 */
	public Stmt getStmt() {
		return stmt;
	}

	/**
	 * @return the sootMethod
	 */
	public SootMethod getSootMethod() {
		return sootMethod;
	}

	final UniqueCodePoint uniqueCodePoint;
	final Stmt stmt;
	private final SootMethod sootMethod;

	@Override
	public String toString() {
		return "UniqueCodePointsAndStmt [sootMethod=" + sootMethod + ", stmt="
				+ stmt + ", uniqueCodePoint=" + uniqueCodePoint + "]";
	}

	public UniqueCodePointsAndStmt(UniqueCodePoint uniqueCodePoint, Stmt stmt,
			SootMethod sootMethod) {
		super();
		this.uniqueCodePoint = uniqueCodePoint;
		this.stmt = stmt;
		this.sootMethod = sootMethod;
	}
}
