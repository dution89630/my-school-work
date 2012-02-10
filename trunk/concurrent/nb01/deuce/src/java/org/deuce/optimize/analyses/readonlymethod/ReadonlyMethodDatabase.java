package org.deuce.optimize.analyses.readonlymethod;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.utils.CollectionUtils;
import org.deuce.optimize.utils.Predicate;

import soot.SootMethod;

public class ReadonlyMethodDatabase implements IDatabase {
	private static ReadonlyMethodDatabase instance = new ReadonlyMethodDatabase();

	public static ReadonlyMethodDatabase getInstance() {
		return instance;
	}

	private final Map<SootMethod, ReadonlyMethodLatticeElement> map = new LinkedHashMap<SootMethod, ReadonlyMethodLatticeElement>();

	public void put(SootMethod sootMethod, ReadonlyMethodLatticeElement element) {
		map.put(sootMethod, element);
	}

	@Override
	public String toString() {
		return "ReadonlyMethodDatabase (only positively readonly methods shown): "
				+ CollectionUtils
						.filterMap(
								map,
								new Predicate<Map.Entry<SootMethod, ReadonlyMethodLatticeElement>>() {
									@Override
									public boolean want(
											Map.Entry<SootMethod, ReadonlyMethodLatticeElement> item) {
										return item.getValue() == ReadonlyMethodLatticeElement.ReadOnly;
									}
								});
	}

	public Map<SootMethod, ReadonlyMethodLatticeElement> getMap() {
		return Collections.unmodifiableMap(map);
	}

	@Override
	public void clear() {
		map.clear();
	}

}
