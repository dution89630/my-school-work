package org.deuce.optimize.utils;

import soot.SootMethod;

public final class ApplicationClassPredicate implements Predicate<SootMethod> {
	@Override
	public boolean want(SootMethod method) {
		return method.getDeclaringClass().isApplicationClass();
	}
}