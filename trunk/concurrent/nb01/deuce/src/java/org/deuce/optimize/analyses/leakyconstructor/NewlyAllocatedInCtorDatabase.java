package org.deuce.optimize.analyses.leakyconstructor;

import java.util.LinkedHashSet;
import java.util.Set;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;

public class NewlyAllocatedInCtorDatabase implements IDatabase,
		ICodePointDatabase {

	private static NewlyAllocatedInCtorDatabase instance = new NewlyAllocatedInCtorDatabase();

	public static NewlyAllocatedInCtorDatabase getInstance() {
		return instance;
	}

	private final Set<UniqueCodePoint> set;

	private NewlyAllocatedInCtorDatabase() {
		set = new LinkedHashSet<UniqueCodePoint>();
	}

	public void add(UniqueCodePoint uniqueCodePoint) {
		set.add(uniqueCodePoint);
	}

	public boolean contains(UniqueCodePoint uniqueCodePoint) {
		return set.contains(uniqueCodePoint);
	}

	@Override
	public String toString() {
		return "NewlyAllocatedInCtorDatabase [set=" + set + "]";
	}

	@Override
	public void clear() {
		set.clear();
	}

}
