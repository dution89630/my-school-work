package org.deuce.optimize.analyses.leakyconstructor;

public enum LeakyConstructorLatticeElement {
	Unknown, NotLeaking, Leaking;

	public static LeakyConstructorLatticeElement merge(
			LeakyConstructorLatticeElement element1,
			LeakyConstructorLatticeElement element2) {
		assert element1 != null;
		assert element2 != null;

		// both are the same ==> return that element 
		if (element1 == element2)
			return element1;

		// one is unknown ==> return the other
		if (element1 == Unknown)
			return element2;
		if (element2 == Unknown)
			return element1;

		// they are different ==> return Leaking
		return Leaking;
	}
}
