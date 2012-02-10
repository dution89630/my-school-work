package org.deuce.optimize.analyses.stable;

import java.util.LinkedHashSet;
import java.util.Set;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;

import soot.SootMethod;

public class StableReadsDatabase implements ICodePointDatabase {
	private static StableReadsDatabase instance = new StableReadsDatabase();

	public static StableReadsDatabase getInstance() {
		return instance;
	}

	private final Set<UniqueCodePoint> codePoints;
	private final Set<SootMethod> methods;

	private StableReadsDatabase() {
		codePoints = new LinkedHashSet<UniqueCodePoint>();
		methods = new LinkedHashSet<SootMethod>();
	}

	public boolean contains(UniqueCodePoint uniqueCodePoint) {
		return codePoints.contains(uniqueCodePoint);
	}

	public void put(UniqueCodePoint uniqueCodePoint) {
		codePoints.add(uniqueCodePoint);
	}

	@Override
	public void clear() {
		codePoints.clear();
		methods.clear();
	}

	@Override
	public String toString() {
		return "StableReadsDatabase [codePoints=" + codePoints + ", methods="
				+ methods + "]";
	}

	public void put(SootMethod sootMethod) {
		methods.add(sootMethod);
	}

	public LinkedHashSet<SootMethod> getAllMethods() {
		return new LinkedHashSet<SootMethod>(methods);
	}
}
