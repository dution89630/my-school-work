package org.deuce.optimize.analyses.immutables;

import java.util.LinkedHashSet;
import java.util.Set;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;

public class ImmutableFieldsAccessesDatabase implements ICodePointDatabase {
	private static ImmutableFieldsAccessesDatabase instance = new ImmutableFieldsAccessesDatabase();

	public static ImmutableFieldsAccessesDatabase getInstance() {
		return instance;
	}

	private final Set<UniqueCodePoint> set;

	private ImmutableFieldsAccessesDatabase() {
		set = new LinkedHashSet<UniqueCodePoint>();
	}

	public boolean contains(UniqueCodePoint uniqueCodePoint) {
		return set.contains(uniqueCodePoint);
	}

	public void put(UniqueCodePoint uniqueCodePoint) {
		set.add(uniqueCodePoint);
	}

	@Override
	public void clear() {
		set.clear();
	}

	@Override
	public String toString() {
		return "ImmutableFieldsAccessesDatabase [set=" + set + "]";
	}

}
