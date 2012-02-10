package org.deuce.optimize.analyses.rescoping.firstfieldactivity;

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

public class FirstFieldActivityDatabase implements IDatabase,
		ICodePointDatabase {

	private static FirstFieldActivityDatabase instance = new FirstFieldActivityDatabase();
	private final HashMap<UniqueCodePoint, InitPoint> initPoints;
	private final HashMap<UniqueCodePoint, MethodInitPointStatus> methods;

	public FirstFieldActivityDatabase() {
		this.initPoints = new LinkedHashMap<UniqueCodePoint, InitPoint>();
		this.methods = new LinkedHashMap<UniqueCodePoint, MethodInitPointStatus>();
	}

	public static FirstFieldActivityDatabase getInstance() {
		return instance;
	}

	public void put(UniqueCodePoint uniqueCodePoint, InitPoint initPoint) {
		initPoints.put(uniqueCodePoint, initPoint);
	}

	public InitPoint getInitPoint(UniqueCodePoint uniqueCodePoint) {
		return initPoints.get(uniqueCodePoint);
	}

	public void put(SootMethod method, MethodInitPointStatus status) {
		UniqueCodePoint uniqueCodePoint = new UniqueCodePoint(method);
		methods.put(uniqueCodePoint, status);
	}

	public MethodInitPointStatus getMethodStatus(SootMethod method) {
		UniqueCodePoint uniqueCodePoint = new UniqueCodePoint(method);
		return methods.get(uniqueCodePoint);
	}

	public MethodInitPointStatus getMethodStatus(UniqueCodePoint method) {
		return methods.get(method);
	}

	public void clear() {
		this.initPoints.clear();
		this.methods.clear();
	}

	@Override
	public String toString() {
		return "FirstFieldActivityDatabase (showing application classes only): [initPoints="
				+ initPoints
				+ ", methods="
				+ CollectionUtils
						.filterMap(
								methods,
								new Predicate<Map.Entry<UniqueCodePoint, MethodInitPointStatus>>() {
									@Override
									public boolean want(
											Entry<UniqueCodePoint, MethodInitPointStatus> item) {
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
