package org.deuce.optimize.analyses.newlocals;

public enum NewLocalsLatticeElement {
	TopUnknown, DefinitelyNew, BottomSuspectedNotNew;

	public static NewLocalsLatticeElement merge(
			NewLocalsLatticeElement element1, NewLocalsLatticeElement element2) {
		assert element1 != null;
		assert element2 != null;

		// both are the same ==> return that element 
		if (element1 == element2)
			return element1;

		// one is unknown ==> return the other
		if (element1 == TopUnknown)
			return element2;
		if (element2 == TopUnknown)
			return element1;

		// they are different ==> return SuspectedNotNew
		return BottomSuspectedNotNew;

	}
}
