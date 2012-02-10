package org.deuce.optimize.analyses.puremethod;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.utils.ApplicationClassPredicate;
import org.deuce.optimize.utils.CollectionUtils;

import soot.SootMethod;

public class PureMethodsDatabase implements IDatabase {

	private static PureMethodsDatabase instance = new PureMethodsDatabase();
	private final Set<SootMethod> set = new LinkedHashSet<SootMethod>();

	private PureMethodsDatabase() {
		super();
	}

	public static PureMethodsDatabase getInstance() {
		return instance;

	}

	public boolean contains(SootMethod sootMethod) {
		return set.contains(sootMethod);
	}

	public void add(SootMethod sootMethod) {
		set.add(sootMethod);
	}

	public boolean isMethodPure(SootMethod sootMethod) {
		return set.contains(sootMethod);
	}

	public Set<SootMethod> getPureMethods() {
		return Collections.unmodifiableSet(set);
	}

	@Override
	public String toString() {
		return "Pure methods (showing only methods in application classes): "
				+ CollectionUtils.filterSet(set,
						new ApplicationClassPredicate());
	}

	public void remove(SootMethod sootMethod) {
		set.remove(sootMethod);
	}

	@Override
	public void clear() {
		set.clear();
	}
}
