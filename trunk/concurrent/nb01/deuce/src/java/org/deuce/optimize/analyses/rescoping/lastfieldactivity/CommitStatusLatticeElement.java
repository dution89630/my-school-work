package org.deuce.optimize.analyses.rescoping.lastfieldactivity;

public enum CommitStatusLatticeElement {
	DefinitelyCommitted, DefinitelyNotCommitted, PossiblyCommitted;

	// this method is like an "intersection":
	// _all_ incoming backward flows must be definitely not committed, in order to be not committed.
	public static CommitStatusLatticeElement merge(CommitStatusLatticeElement in1,
			CommitStatusLatticeElement in2) {
		// two flows with the same status, yield that status.  
		if (in1 == in2) {
			return in1;
		}
		// if one of them is PossiblyCommitted, then we return PossiblyCommitted.
		// otherwise, the two flows must be DefinitelyCommitted and DefinitelyNotCommitted, in which
		// case we yield PossiblyCommitted.
		return PossiblyCommitted;

	}
}