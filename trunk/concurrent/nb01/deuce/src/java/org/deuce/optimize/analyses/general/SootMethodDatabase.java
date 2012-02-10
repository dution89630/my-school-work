package org.deuce.optimize.analyses.general;

import java.util.LinkedHashMap;
import java.util.Map;

import soot.SootMethod;

public class SootMethodDatabase implements ICodePointDatabase {
	private static SootMethodDatabase instance = new SootMethodDatabase();

	private SootMethodDatabase() {
		super();
	}

	public static SootMethodDatabase getInstance() {
		return instance;
	}

	private final Map<UniqueCodePoint, SootMethod> map = new LinkedHashMap<UniqueCodePoint, SootMethod>();

	public void add(UniqueCodePoint uniqueCodePoint, SootMethod sootMethod) {
		map.put(uniqueCodePoint, sootMethod);
	}

	public SootMethod getSootMethod(UniqueCodePoint uniqueCodePoint) {
		return map.get(uniqueCodePoint);
	}

	@Override
	public String toString() {
		return "SootMethodDatabase [map=" + map + "]";
	}

	@Override
	public void clear() {
		map.clear();
	}

}
