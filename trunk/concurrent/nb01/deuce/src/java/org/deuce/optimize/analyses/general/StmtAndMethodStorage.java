package org.deuce.optimize.analyses.general;

import soot.SootMethod;
import soot.jimple.Stmt;

public class StmtAndMethodStorage {

	private static StmtsDatabase stmtsDatabase = StmtsDatabase.getInstance();
	private static SootMethodDatabase sootMethodDatabase = SootMethodDatabase
			.getInstance();

	public static void put(UniqueCodePoint uniqueCodePoint,
			SootMethod sootMethod, Stmt stmt) {
		stmtsDatabase.add(uniqueCodePoint, stmt);
		sootMethodDatabase.add(uniqueCodePoint, sootMethod);
	}

}
