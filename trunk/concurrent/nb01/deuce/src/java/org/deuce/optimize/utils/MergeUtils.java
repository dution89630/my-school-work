package org.deuce.optimize.utils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class MergeUtils {

	public static <K, E> Map<K, E> mergeMaps(Map<K, E> map1, Map<K, E> map2,
			Mergable<E> mergable) {

		Map<K, E> mergedMap = new LinkedHashMap<K, E>();
		Set<K> allKeys = new LinkedHashSet<K>();
		allKeys.addAll(map1.keySet());
		allKeys.addAll(map2.keySet());

		for (K key : allKeys) {
			E e1 = map1.get(key);
			E e2 = map2.get(key);
			if (e2 == null) {
				// key exists only in map1
				mergedMap.put(key, e1);
			} else if (e1 == null) {
				// key exists only in map2
				mergedMap.put(key, e2);
			} else {
				// key exists in both maps; take merged value
				E mergedE = mergable.merge(e1, e2);
				mergedMap.put(key, mergedE);
			}
		}

		return mergedMap;
	}

}
