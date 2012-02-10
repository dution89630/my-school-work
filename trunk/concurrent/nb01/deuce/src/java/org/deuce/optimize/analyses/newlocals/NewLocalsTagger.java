package org.deuce.optimize.analyses.newlocals;

import java.util.List;

import org.deuce.optimize.analyses.general.StmtAndMethodStorage;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.Logger;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Stmt;

public class NewLocalsTagger {

	private final NewLocalsDatabase newLocalsDatabase;

	public NewLocalsTagger() {
		newLocalsDatabase = NewLocalsDatabase.getInstance();

	}

	public void tag(Body activeBody, NewLocalsIntraprocAnalysis analysis) {
		PatchingChain<Unit> units = activeBody.getUnits();
		for (Unit unit : units) {
			Stmt stmt = (Stmt) unit;
			if (stmt.containsFieldRef()) {
				FieldRef fieldRef = stmt.getFieldRef();
				if (fieldRef instanceof InstanceFieldRef) {
					InstanceFieldRef instanceFieldRef = (InstanceFieldRef) fieldRef;
					Local base = (Local) instanceFieldRef.getBase();
					NewLocalsFlowSet flowSet = analysis.getFlowAfter(unit);
					ValueNumber valueNumber = flowSet.getValueNumber(base);
					NewLocalsLatticeElement latticeElement = flowSet
							.getLatticeElement(valueNumber);
					if (latticeElement == NewLocalsLatticeElement.DefinitelyNew)
						addToDatabase(activeBody.getMethod()
								.getDeclaringClass(), activeBody.getMethod(),
								stmt);
				}
			}
		}
	}

	private void addToDatabase(SootClass sootClass, SootMethod sootMethod,
			Stmt stmt) {
		List<UniqueCodePoint> uniqueCodePoints = UniqueCodePoint
				.generateUniqueCodePointsFor(sootMethod, stmt);
		for (UniqueCodePoint uniqueCodePoint : uniqueCodePoints) {
			newLocalsDatabase.add(uniqueCodePoint);
			StmtAndMethodStorage.put(uniqueCodePoint, sootMethod, stmt);
		}

		Logger.println("NLT: " + sootMethod + ": " + stmt
				+ " can be optimized.", sootMethod);
	}

}
