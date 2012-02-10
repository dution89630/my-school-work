package org.deuce.optimize.analyses.fieldactivity.backwards;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.deuce.optimize.analyses.fieldactivity.AllocNodeAndField;
import org.deuce.optimize.analyses.fieldactivity.FieldActivityOperation;
import org.deuce.optimize.utils.Mergable;
import org.deuce.optimize.utils.MergeUtils;

public class DeprivedWriteFlowSet {

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FieldActivityFlowSet [map=" + map + "]";
	}

	private final Map<AllocNodeAndField, DeprivedWriteLatticeElement> map;

	public Map<AllocNodeAndField, DeprivedWriteLatticeElement> getMapCopy() {
		return Collections.unmodifiableMap(map);
	}

	public DeprivedWriteFlowSet() {
		map = new LinkedHashMap<AllocNodeAndField, DeprivedWriteLatticeElement>();
	}

	public void copy(DeprivedWriteFlowSet dest) {
		dest.map.clear();
		dest.map.putAll(this.map);
	}

	public static DeprivedWriteFlowSet merge(DeprivedWriteFlowSet in1,
			DeprivedWriteFlowSet in2) {
		DeprivedWriteFlowSet merged = new DeprivedWriteFlowSet();

		Map<AllocNodeAndField, DeprivedWriteLatticeElement> mergedMap = MergeUtils
				.mergeMaps(in1.map, in2.map,
						new Mergable<DeprivedWriteLatticeElement>() {
							@Override
							public DeprivedWriteLatticeElement merge(
									DeprivedWriteLatticeElement element1,
									DeprivedWriteLatticeElement element2) {
								return DeprivedWriteLatticeElement.merge(
										element1, element2);
							}
						});
		merged.map.putAll(mergedMap);

		return merged;
	}

	public void mergeInto(DeprivedWriteFlowSet dest) {
		DeprivedWriteFlowSet merged = merge(this, dest);
		merged.copy(dest);
	}

	public void putAndMerge(AllocNodeAndField allocNodeAndField,
			DeprivedWriteLatticeElement element, boolean isApplicationClass) {
		DeprivedWriteLatticeElement element2 = map.get(allocNodeAndField);
		if (element2 == null) {
			if (isApplicationClass) {
				map.put(allocNodeAndField, element);
			}
		} else {
			map.put(allocNodeAndField, DeprivedWriteLatticeElement.merge(
					element, element2));

		}
	}

	public void applyAndMerge(AllocNodeAndField allocNodeAndField,
			FieldActivityOperation operation, boolean isApplicationClass) {
		DeprivedWriteLatticeElement currentElement = map.get(allocNodeAndField);
		DeprivedWriteLatticeElement newElement = DeprivedWriteLatticeElement
				.applyOperation(currentElement, operation);
		putAndMerge(allocNodeAndField, newElement, isApplicationClass);
	}

	public DeprivedWriteLatticeElement get(AllocNodeAndField allocNodeAndField) {
		return map.get(allocNodeAndField);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DeprivedWriteFlowSet other = (DeprivedWriteFlowSet) obj;
		if (map == null) {
			if (other.map != null) {
				return false;
			}
		} else if (!map.equals(other.map)) {
			return false;
		}
		return true;
	}

}
