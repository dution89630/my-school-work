package org.deuce.optimize.analyses.alwaysatomic;

import java.util.LinkedHashSet;
import java.util.Set;

import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.utils.ApplicationClassPredicate;
import org.deuce.optimize.utils.CollectionUtils;

import soot.SootMethod;

public class NotAlwaysAtomicDatabase implements IDatabase {
	private static NotAlwaysAtomicDatabase instance = new NotAlwaysAtomicDatabase();
	private final Set<SootMethod> set = new LinkedHashSet<SootMethod>();

	private NotAlwaysAtomicDatabase() {
		super();
	}

	public static NotAlwaysAtomicDatabase getInstance() {
		return instance;

	}

	public boolean contains(SootMethod sootMethod) {
		return set.contains(sootMethod);
	}

	public void add(SootMethod sootMethod) {
		set.add(sootMethod);
	}

	public boolean isMethodAlwaysAtomic(SootMethod sootMethod) {
		return !set.contains(sootMethod);
	}

	public void clear() {
		set.clear();
	}

	@Override
	public String toString() {
		return "Methods not always atomic (showing only methods from application classes): "
				+ CollectionUtils.filterSet(set,
						new ApplicationClassPredicate());
	}
}
