package org.deuce.optimize.analyses.threadescape;

import java.util.LinkedHashSet;
import java.util.Set;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;

import soot.jimple.spark.pag.AllocNode;

public class ThreadEscapeDatabase implements IDatabase, ICodePointDatabase {

	@Override
	public String toString() {
		return "ThreadEscapeDatabase [accessesSet=" + accessesSet
				+ ", objectsSet=" + objectsSet + "]";
	}

	private static ThreadEscapeDatabase instance = new ThreadEscapeDatabase();

	private ThreadEscapeDatabase() {
	}
	
	public static ThreadEscapeDatabase getInstance() {
		return instance;
	}

	private final Set<AllocNode> objectsSet = new LinkedHashSet<AllocNode>();
	private final Set<UniqueCodePoint> accessesSet = new LinkedHashSet<UniqueCodePoint>();

	public void addToThreadEscapingObjects(AllocNode allocNode) {
		objectsSet.add(allocNode);
	}

	public boolean containsObject(AllocNode allocNode) {
		return objectsSet.contains(allocNode);
	}

	public void addToThreadEscapingAccesses(UniqueCodePoint uniqueCodePoint) {
		accessesSet.add(uniqueCodePoint);
	}

	public boolean isAccessThreadEscaping(UniqueCodePoint uniqueCodePoint) {
		boolean threadEscaping = accessesSet.contains(uniqueCodePoint);
		return threadEscaping;
	}

	public boolean isEmpty() {
		return objectsSet.isEmpty() && accessesSet.isEmpty();
	}

	@Override
	public void clear() {
		this.accessesSet.clear();
		this.objectsSet.clear();
	}
}
