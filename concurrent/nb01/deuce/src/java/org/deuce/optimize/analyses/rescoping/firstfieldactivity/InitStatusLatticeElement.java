package org.deuce.optimize.analyses.rescoping.firstfieldactivity;

public enum InitStatusLatticeElement {
	// a. no init necessary yet
	// b. definitely inited already
	// c. possibly inited already
	NoInitNecessary, DefinitelyInitedAlready, PossiblyInitedAlready;

	// this method is like an "intersection":
	// _all_ incoming flows must be definitely inited, in order to be inited.
	public static InitStatusLatticeElement merge(InitStatusLatticeElement in1,
			InitStatusLatticeElement in2) {
		// two flows with the same status, yield that status.  
		if (in1 == in2) {
			return in1;
		}
		// if one of them is PossiblyInitedAlready, then we return PossiblyInitedAlready.
		// otherwise, the two flows must be NoInitNecessary and DefinitelyInitedAlready, in which
		// case we yield PossibleInitedAlready.
		return PossiblyInitedAlready;

	}

}