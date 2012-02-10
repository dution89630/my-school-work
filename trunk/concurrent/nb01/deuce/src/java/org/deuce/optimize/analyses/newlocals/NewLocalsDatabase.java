package org.deuce.optimize.analyses.newlocals;

import java.util.HashSet;
import java.util.LinkedHashSet;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;

public class NewLocalsDatabase implements IDatabase, ICodePointDatabase {

	private static NewLocalsDatabase instance = new NewLocalsDatabase();
	private final HashSet<UniqueCodePoint> set;

	public NewLocalsDatabase() {
		this.set = new LinkedHashSet<UniqueCodePoint>();
	}

	public static NewLocalsDatabase getInstance() {
		return instance;
	}

	public void add(UniqueCodePoint uniqueCodePoint) {
		set.add(uniqueCodePoint);
	}

	public boolean contains(UniqueCodePoint uniqueCodePoint) {
		return set.contains(uniqueCodePoint);
	}

	@Override
	public String toString() {
		return "NewLocalsDatabase [set=" + set + "]";
	}

	@Override
	public void clear() {
		set.clear();
	}

}
