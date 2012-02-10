package org.deuce.optimize.analyses.general;

import java.util.LinkedHashMap;
import java.util.Map;

import soot.jimple.Stmt;

public class StmtsDatabase implements ICodePointDatabase {
	private static StmtsDatabase instance = new StmtsDatabase();

	private StmtsDatabase() {
		super();
	}

	public static StmtsDatabase getInstance() {
		return instance;
	}

	private final Map<UniqueCodePoint, Stmt> map = new LinkedHashMap<UniqueCodePoint, Stmt>();

	public void add(UniqueCodePoint uniqueCodePoint, Stmt stmt) {
		map.put(uniqueCodePoint, stmt);
	}

	public Stmt getStmt(UniqueCodePoint uniqueCodePoint) {
		return map.get(uniqueCodePoint);
	}

	@Override
	public String toString() {
		return "StmtsDatabase [map=" + map + "]";
	}

	@Override
	public void clear() {
		map.clear();
	}

}
