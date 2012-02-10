package org.deuce.optimize.analyses.readonlymethod;

import java.util.HashSet;
import java.util.LinkedHashSet;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;

public class ReadonlyActivityDatabase implements IDatabase, ICodePointDatabase {

	private static ReadonlyActivityDatabase instance = new ReadonlyActivityDatabase();
	private final HashSet<UniqueCodePoint> set;

	public ReadonlyActivityDatabase() {
		this.set = new LinkedHashSet<UniqueCodePoint>();
	}

	public static ReadonlyActivityDatabase getInstance() {
		return instance;
	}

	public void put(UniqueCodePoint uniqueCodePoint) {
		set.add(uniqueCodePoint);
	}

	public boolean isReadonlyActivity(UniqueCodePoint uniqueCodePoint) {
		return set.contains(uniqueCodePoint);
	}

	@Override
	public String toString() {
		return "ReadonlyActivityDatabase [set=" + set + "]";
	}

	@Override
	public void clear() {
		set.clear();
	}

}
