package org.deuce.optimize.utils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class CollectionUtils {

	public static <T> Set<T> filterSet(Set<T> set, Predicate<T> want) {
		LinkedHashSet<T> returnSet = new LinkedHashSet<T>();
		for (T t : set) {
			if (want.want(t)) {
				returnSet.add(t);
			}
		}
		return returnSet;
	}

	public static <K, V> Map<K, V> filterMap(Map<K, V> map,
			Predicate<Entry<K, V>> want) {
		LinkedHashMap<K, V> returnMap = new LinkedHashMap<K, V>();
		for (Entry<K, V> entry : map.entrySet()) {
			if (want.want(entry)) {
				returnMap.put(entry.getKey(), entry.getValue());
			}
		}
		return returnMap;
	}

}
