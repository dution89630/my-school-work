package org.deuce.optimize.analyses.newlyallocated;

public enum NewlyAllocatedLatticeElement {
	Unknown, DefinitelyNew, SuspectedNotNew;

	public static NewlyAllocatedLatticeElement takeWorse(
			NewlyAllocatedLatticeElement element1,
			NewlyAllocatedLatticeElement element2) {
		assert element1 != null;
		assert element2 != null;

		if ((element1 == SuspectedNotNew) || (element2 == SuspectedNotNew))
			return SuspectedNotNew;
		else if ((element1 == DefinitelyNew) || (element2 == DefinitelyNew))
			return DefinitelyNew;
		return Unknown;
	}
}
