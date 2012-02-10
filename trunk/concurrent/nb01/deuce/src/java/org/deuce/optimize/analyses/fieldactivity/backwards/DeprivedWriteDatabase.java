package org.deuce.optimize.analyses.fieldactivity.backwards;

import java.util.LinkedHashMap;
import java.util.Map;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;

public class DeprivedWriteDatabase implements IDatabase, ICodePointDatabase {
	private static DeprivedWriteDatabase instance = new DeprivedWriteDatabase();

	public static DeprivedWriteDatabase getInstance() {
		return instance;
	}

	private final Map<UniqueCodePoint, DeprivedWriteLatticeElement> mapAtPoint = new LinkedHashMap<UniqueCodePoint, DeprivedWriteLatticeElement>();
	private final Map<UniqueCodePoint, DeprivedWriteLatticeElement> mapAtEnd = new LinkedHashMap<UniqueCodePoint, DeprivedWriteLatticeElement>();

	public void add(UniqueCodePoint uniqueCodePoint,
			DeprivedWriteLatticeElement elementAtPoint,
			DeprivedWriteLatticeElement elementAtEnd) {
		mapAtPoint.put(uniqueCodePoint, elementAtPoint);
		mapAtEnd.put(uniqueCodePoint, elementAtEnd);
	}

	public DeprivedWriteLatticeElement getFieldActivityAtCodePoint(
			UniqueCodePoint uniqueCodePoint) {
		return mapAtPoint.get(uniqueCodePoint);
	}

	public DeprivedWriteLatticeElement getFieldActivityAtEnd(
			UniqueCodePoint uniqueCodePoint) {
		return mapAtEnd.get(uniqueCodePoint);
	}

	public void clear() {
		mapAtPoint.clear();
		mapAtEnd.clear();
	}

	@Override
	public String toString() {
		return "FieldActivityDatabase [mapAtPoint=" + mapAtPoint
				+ ", mapAtEnd=" + mapAtEnd + "]";
	}

}
