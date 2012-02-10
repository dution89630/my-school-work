package org.deuce.optimize.analyses.rescoping.lastfieldactivity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.CollectionUtils;
import org.deuce.optimize.utils.Predicate;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class CommitPointDatabase implements IDatabase, ICodePointDatabase {

	private static CommitPointDatabase instance = new CommitPointDatabase();
	private final HashMap<UniqueCodePoint, CommitPoint> commitPoints;
	private final HashMap<UniqueCodePoint, MethodCommitPointStatus> methods;

	public CommitPointDatabase() {
		this.commitPoints = new LinkedHashMap<UniqueCodePoint, CommitPoint>();
		this.methods = new LinkedHashMap<UniqueCodePoint, MethodCommitPointStatus>();
	}

	public static CommitPointDatabase getInstance() {
		return instance;
	}

	public void put(UniqueCodePoint uniqueCodePoint, CommitPoint initPoint) {
		commitPoints.put(uniqueCodePoint, initPoint);
	}

	public CommitPoint getCommitPoint(UniqueCodePoint uniqueCodePoint) {
		return commitPoints.get(uniqueCodePoint);
	}

	public void put(SootMethod method, MethodCommitPointStatus status) {
		UniqueCodePoint uniqueCodePoint = new UniqueCodePoint(method);
		methods.put(uniqueCodePoint, status);
	}

	public MethodCommitPointStatus getMethodStatus(SootMethod method) {
		UniqueCodePoint uniqueCodePoint = new UniqueCodePoint(method);
		return methods.get(uniqueCodePoint);
	}

	public MethodCommitPointStatus getMethodStatus(UniqueCodePoint method) {
		return methods.get(method);
	}

	public void clear() {
		this.commitPoints.clear();
		this.methods.clear();
	}

	@Override
	public String toString() {
		return "CommitPointDatabase (showing application classes only): [commitPoints="
				+ commitPoints
				+ ", methods="
				+ CollectionUtils
						.filterMap(
								methods,
								new Predicate<Map.Entry<UniqueCodePoint, MethodCommitPointStatus>>() {
									@Override
									public boolean want(
											Entry<UniqueCodePoint, MethodCommitPointStatus> item) {
										SootClass sootClass = Scene
												.v()
												.getSootClass(
														item.getKey()
																.getClassName());
										return sootClass.isApplicationClass();
									}
								}) + "]";
	}
}
