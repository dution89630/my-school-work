package org.deuce.optimize.analyses.immutables;

import soot.Scene;
import soot.SootClass;
import soot.SootField;

public class AllFieldsFinder {

	public void find() {
		FieldsMutabilityDatabase database = FieldsMutabilityDatabase.getInstance();

		for (SootClass sootClass : Scene.v().getApplicationClasses()) {
			for (SootField sootField : sootClass.getFields()) {
				database.addField(sootField);
			}
		}

	}
}
