package org.deuce.optimize.analyses.fieldactivity;

import java.util.LinkedHashMap;
import java.util.Map;

import org.deuce.optimize.analyses.fieldactivity.backwards.DeprivedWriteLatticeElement;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadLatticeElement;
import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;

public class FieldActivityDatabase implements IDatabase, ICodePointDatabase {
	private static FieldActivityDatabase instance = new FieldActivityDatabase();

	public static FieldActivityDatabase getInstance() {
		return instance;
	}

	private final Map<UniqueCodePoint, VirginReadLatticeElement> virginReadsMap = new LinkedHashMap<UniqueCodePoint, VirginReadLatticeElement>();
	private final Map<UniqueCodePoint, DeprivedWriteLatticeElement> deprivedWritesMap = new LinkedHashMap<UniqueCodePoint, DeprivedWriteLatticeElement>();

	public void add(UniqueCodePoint uniqueCodePoint,
			VirginReadLatticeElement virginReadElement,
			DeprivedWriteLatticeElement deprivedWriteElement) {
		virginReadsMap.put(uniqueCodePoint, virginReadElement);
		deprivedWritesMap.put(uniqueCodePoint, deprivedWriteElement);
	}

	public VirginReadLatticeElement getVirginReadElement(
			UniqueCodePoint uniqueCodePoint) {
		return virginReadsMap.get(uniqueCodePoint);
	}

	public DeprivedWriteLatticeElement getDeprivedWriteElement(
			UniqueCodePoint uniqueCodePoint) {
		return deprivedWritesMap.get(uniqueCodePoint);
	}

	public void clear() {
		virginReadsMap.clear();
		deprivedWritesMap.clear();
	}

	@Override
	public String toString() {
		return "FieldActivityDatabase [virginReadsMap=" + virginReadsMap
				+ ", deprivedWritesMap=" + deprivedWritesMap + "]";
	}

}
