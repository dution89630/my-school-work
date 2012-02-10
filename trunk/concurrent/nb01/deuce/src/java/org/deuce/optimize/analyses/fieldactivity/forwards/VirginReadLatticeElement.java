package org.deuce.optimize.analyses.fieldactivity.forwards;

import org.deuce.optimize.analyses.fieldactivity.FieldActivityOperation;
import org.deuce.optimize.utils.OptimizerException;

public enum VirginReadLatticeElement {
	TopUnknown, // not read nor written yet
	ReadButNotYetWrittenTo, // was read at least once, but not written to 
	BottomNotReadOnly; // written to

	public static VirginReadLatticeElement merge(
			VirginReadLatticeElement element1,
			VirginReadLatticeElement element2) {
		if (element1 == null)
			throw new OptimizerException("element1 cannot be null.");
		if (element2 == null)
			throw new OptimizerException("element2 cannot be null.");

		int max = Math.max(element1.ordinal(), element2.ordinal());
		return values()[max];
	}

	// this method applies a kind of a transfer in a finite state automation.
	public static VirginReadLatticeElement applyOperation(
			VirginReadLatticeElement element,
			FieldActivityOperation operation) {
		if (element == null) {
			element = TopUnknown;
		}

		switch (element) {
		case TopUnknown:
			switch (operation) {
			case Read:
				return ReadButNotYetWrittenTo;
			case Write:
				return BottomNotReadOnly;
			}

		case ReadButNotYetWrittenTo:
			switch (operation) {
			case Read:
				return ReadButNotYetWrittenTo;
			case Write:
				return BottomNotReadOnly;
			}
	
		case BottomNotReadOnly:
			return BottomNotReadOnly;
		}
		throw new OptimizerException(String.format(
				"Bad element (%s) or opreation (%s)", element, operation));
	}
}
