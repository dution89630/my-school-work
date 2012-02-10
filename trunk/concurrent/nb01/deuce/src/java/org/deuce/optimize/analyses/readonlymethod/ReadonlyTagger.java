package org.deuce.optimize.analyses.readonlymethod;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.deuce.optimize.analyses.general.StmtAndMethodStorage;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.Logger;

import soot.Body;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public class ReadonlyTagger {

	private final ReadonlyActivityDatabase readonlyActivityDatabase = ReadonlyActivityDatabase
			.getInstance();

	public void tag() {
		Map<SootMethod, ReadonlyMethodLatticeElement> map = ReadonlyMethodDatabase
				.getInstance().getMap();
		Set<Entry<SootMethod, ReadonlyMethodLatticeElement>> entrySet = map
				.entrySet();
		for (Entry<SootMethod, ReadonlyMethodLatticeElement> entry : entrySet) {
			if (entry.getValue() == ReadonlyMethodLatticeElement.ReadOnly) {
				SootMethod sootMethod = entry.getKey();
				Body body = sootMethod.getActiveBody();
				PatchingChain<Unit> units = body.getUnits();
				for (Unit unit : units) {
					Stmt stmt = (Stmt) unit;
					if (stmt.containsFieldRef() || stmt.containsArrayRef()
							|| stmt.containsInvokeExpr()) {
						addToDatabase(sootMethod.getDeclaringClass(),
								sootMethod, stmt);
					}
				}
			}
		}
	}

	private void addToDatabase(SootClass sootClass, SootMethod sootMethod,
			Stmt stmt) {
		List<UniqueCodePoint> uniqueCodePoints = UniqueCodePoint
				.generateUniqueCodePointsFor(sootMethod, stmt);
		for (UniqueCodePoint uniqueCodePoint : uniqueCodePoints) {
			readonlyActivityDatabase.put(uniqueCodePoint);
			StmtAndMethodStorage.put(uniqueCodePoint, sootMethod, stmt);
		}

		Logger.println("ROT: " + sootMethod + ": " + stmt
				+ " is a read-only activity.", sootMethod);
	}
}
