package org.deuce.optimize.analyses.immutables;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.deuce.optimize.analyses.general.IDatabase;
import org.deuce.optimize.utils.OptimizerException;

import soot.SootClass;
import soot.SootField;

public class FieldsMutabilityDatabase implements IDatabase {

	public enum FieldStatus {
		Mutable, Immutable
	}

	private static FieldsMutabilityDatabase instance = new FieldsMutabilityDatabase();

	public static FieldsMutabilityDatabase getInstance() {
		return instance;
	}

	private final Map<SootField, FieldStatus> map = new LinkedHashMap<SootField, FieldStatus>();

	public void addField(SootField sootField) {
		//if (map.containsKey(sootField))
		//throw new OptimizerException("Field %s already exists.", sootField
		//	.toString());
		map.put(sootField, FieldStatus.Immutable);
	}

	public void setFieldAsMutable(SootField sootField) {
		if (!map.containsKey(sootField))
			throw new OptimizerException("Field %s not found.", sootField
					.toString());
		map.put(sootField, FieldStatus.Mutable);
	}

	public List<SootField> getAllImmutableFields() {
		Set<Entry<SootField, FieldStatus>> entrySet = map.entrySet();
		List<SootField> list = new ArrayList<SootField>();
		for (Entry<SootField, FieldStatus> entry : entrySet) {
			if (entry.getValue() == FieldStatus.Immutable) {
				list.add(entry.getKey());
			}
		}
		return list;
	}

	public void setAllFieldsInClassAsMutable(SootClass sootClass) {
		for (SootField sootField : sootClass.getFields()) {
			setFieldAsMutable(sootField);
		}

	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public String toString() {
		return "FieldsDatabase [map=" + map + "]";
	}

}