package org.deuce.optimize.analyses.leakyconstructor;

import java.util.LinkedHashSet;
import java.util.Set;

import org.deuce.optimize.analyses.general.IDatabase;

import soot.SootClass;
import soot.SootMethod;

public class LeakyConstructorsDatabase implements IDatabase {

	private static LeakyConstructorsDatabase instance = new LeakyConstructorsDatabase();

	public static LeakyConstructorsDatabase getInstance() {
		return instance;
	}

	private final LinkedHashSet<SootMethod> ctors;
	private final LinkedHashSet<SootClass> classes;

	private LeakyConstructorsDatabase() {
		this.ctors = new LinkedHashSet<SootMethod>();
		this.classes = new LinkedHashSet<SootClass>();
	}

	public void putLeakyConstructor(SootMethod method) {
		ctors.add(method);
		classes.add(method.getDeclaringClass());
	}

	@Override
	public String toString() {
		Set<SootMethod> partialCtors = new LinkedHashSet<SootMethod>();
		for (SootMethod sootMethod : ctors) {
			if (sootMethod.getDeclaringClass().isApplicationClass()) {
				partialCtors.add(sootMethod);
			}
		}
		Set<SootClass> partialClasses = new LinkedHashSet<SootClass>();
		for (SootClass sootClass : classes) {
			if (sootClass.isApplicationClass()) {
				partialClasses.add(sootClass);
			}
		}

		return "LeakyConstructorsDatabase (showing only application classes and methods): [classes="
				+ partialClasses + ", ctors=" + partialCtors + "]";
	}

	public boolean isConstructorLeaky(SootMethod sootMethod) {
		return ctors.contains(sootMethod);
	}

	public boolean hasLeakyConstructor(SootClass sootClass) {
		return classes.contains(sootClass);
	}

	@Override
	public void clear() {
		classes.clear();
		ctors.clear();
	}
}
