package org.deuce.optimize.analyses.newlyallocated;

import java.util.HashSet;
import java.util.LinkedHashSet;

import org.deuce.optimize.analyses.general.ICodePointDatabase;
import org.deuce.optimize.analyses.general.IDatabase;

public class NewlyAllocatedDatabase implements IDatabase, ICodePointDatabase {

	private static NewlyAllocatedDatabase instance = new NewlyAllocatedDatabase();
	private final HashSet<String> set;

	public NewlyAllocatedDatabase() {
		this.set = new LinkedHashSet<String>();
	}

	public static NewlyAllocatedDatabase getInstance() {
		return instance;
	}

	public void put(String className, String methodName, int bytecodeOffset) {
		set.add(className + "." + methodName + ":" + bytecodeOffset);
	}

	public boolean contains(String className, String methodName,
			int bytecodeOffset) {
		return set
				.contains(className + "." + methodName + ":" + bytecodeOffset);
	}

	@Override
	public void clear() {
		set.clear();
	}

}
