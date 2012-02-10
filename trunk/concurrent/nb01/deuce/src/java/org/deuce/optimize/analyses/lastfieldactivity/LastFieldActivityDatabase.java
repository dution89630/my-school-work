package org.deuce.optimize.analyses.lastfieldactivity;

import java.util.HashSet;
import java.util.LinkedHashSet;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;

public class LastFieldActivityDatabase implements IDatabase, ICodePointDatabase {

	private static LastFieldActivityDatabase instance = new LastFieldActivityDatabase();
	private final HashSet<UniqueCodePoint> set;

	public LastFieldActivityDatabase() {
		this.set = new LinkedHashSet<UniqueCodePoint>();
	}

	public static LastFieldActivityDatabase getInstance() {
		return instance;
	}

	public void put(UniqueCodePoint uniqueCodePoint) {
		set.add(uniqueCodePoint);
	}

	public boolean isLastActivity(UniqueCodePoint uniqueCodePoint) {
		return set.contains(uniqueCodePoint);
	}

	@Override
	public void clear() {
		set.clear();
	}

	@Override
	public String toString() {
		return "LastFieldActivityDatabase [set=" + set + "]";
	}

}
