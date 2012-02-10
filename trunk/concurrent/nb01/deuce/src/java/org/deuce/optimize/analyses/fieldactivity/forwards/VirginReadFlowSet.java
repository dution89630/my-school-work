package org.deuce.optimize.analyses.fieldactivity.forwards;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.deuce.optimize.analyses.fieldactivity.AllocNodeAndField;
import org.deuce.optimize.analyses.fieldactivity.FieldActivityOperation;
import org.deuce.optimize.utils.Mergable;
import org.deuce.optimize.utils.MergeUtils;

public class VirginReadFlowSet {

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FieldActivityFlowSet [map=" + map + "]";
	}

	private final Map<AllocNodeAndField, VirginReadLatticeElement> map;

	public Map<AllocNodeAndField, VirginReadLatticeElement> getMapCopy() {
		return Collections.unmodifiableMap(map);
	}

	public VirginReadFlowSet() {
		map = new LinkedHashMap<AllocNodeAndField, VirginReadLatticeElement>();
	}

	public void copy(VirginReadFlowSet dest) {
		dest.map.clear();
		dest.map.putAll(this.map);
	}

	public static VirginReadFlowSet merge(VirginReadFlowSet in1,
			VirginReadFlowSet in2) {
		VirginReadFlowSet merged = new VirginReadFlowSet();

		Map<AllocNodeAndField, VirginReadLatticeElement> mergedMap = MergeUtils
				.mergeMaps(in1.map, in2.map,
						new Mergable<VirginReadLatticeElement>() {
							@Override
							public VirginReadLatticeElement merge(
									VirginReadLatticeElement element1,
									VirginReadLatticeElement element2) {
								return VirginReadLatticeElement.merge(
										element1, element2);
							}
						});
		merged.map.putAll(mergedMap);

		return merged;
	}

	public void mergeInto(VirginReadFlowSet dest) {
		VirginReadFlowSet merged = merge(this, dest);
		merged.copy(dest);
	}

	public void putAndMerge(AllocNodeAndField allocNodeAndField,
			VirginReadLatticeElement element, boolean isApplicationClass) {
		VirginReadLatticeElement element2 = map.get(allocNodeAndField);
		if (element2 == null) {
			if (isApplicationClass) {
				map.put(allocNodeAndField, element);
			}
		} else {
			map.put(allocNodeAndField, VirginReadLatticeElement.merge(
					element, element2));

		}
	}

	public void applyAndMerge(AllocNodeAndField allocNodeAndField,
			FieldActivityOperation operation, boolean isApplicationClass) {
		VirginReadLatticeElement currentElement = map.get(allocNodeAndField);
		VirginReadLatticeElement newElement = VirginReadLatticeElement
				.applyOperation(currentElement, operation);
		putAndMerge(allocNodeAndField, newElement, isApplicationClass);
	}

	public VirginReadLatticeElement get(AllocNodeAndField allocNodeAndField) {
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
		VirginReadFlowSet other = (VirginReadFlowSet) obj;
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
