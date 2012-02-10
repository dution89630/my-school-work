package org.deuce.optimize.analyses.atomicstarter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.utils.CollectionUtils;
import org.deuce.optimize.utils.Predicate;

import soot.SootClass;
import soot.SootMethod;

public class AtomicStartersDatabase implements IDatabase {
	private static AtomicStartersDatabase instance = new AtomicStartersDatabase();
	private final Map<SootMethod, AtomicStartersElement> map = new LinkedHashMap<SootMethod, AtomicStartersElement>();

	private AtomicStartersDatabase() {
		super();
	}

	public static AtomicStartersDatabase getInstance() {
		return instance;
	}

	public boolean isMethodAtomicStarter(SootMethod sootMethod) {
		return map.get(sootMethod) == AtomicStartersElement.AtomicAndNotCalledFromAtomic;
	}

	public boolean isMethodInScope(SootMethod sootMethod) {
		return map.containsKey(sootMethod);
	}

	@Override
	public String toString() {
		return "Atomic starters database (only methods from application classes shown): "
				+ CollectionUtils
						.filterMap(
								map,
								new Predicate<Map.Entry<SootMethod, AtomicStartersElement>>() {
									@Override
									public boolean want(
											Entry<SootMethod, AtomicStartersElement> item) {
										SootClass sootClass = item.getKey()
												.getDeclaringClass();
										return sootClass.isApplicationClass();
									}
								});
	}

	public void put(SootMethod sootMethod, AtomicStartersElement element) {
		map.put(sootMethod, element);
	}

	public void clear() {
		map.clear();
	}
}