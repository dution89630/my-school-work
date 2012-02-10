package org.deuce.optimize.analyses.fieldactivity.backwards;

import org.deuce.optimize.analyses.fieldactivity.FieldActivityOperation;
import org.deuce.optimize.utils.OptimizerException;

public enum DeprivedWriteLatticeElement {
	TopUnknown, // not read nor written ever
	WillBeWrittenButNotReadThereafter, // was written to at least once 
	BottomWillBeRead; // read after write was encountered

	public static DeprivedWriteLatticeElement merge(
			DeprivedWriteLatticeElement element1,
			DeprivedWriteLatticeElement element2) {
		if (element1 == null)
			throw new OptimizerException("element1 cannot be null.");
		if (element2 == null)
			throw new OptimizerException("element2 cannot be null.");

		int max = Math.max(element1.ordinal(), element2.ordinal());
		return values()[max];
	}

	// this method applies a kind of a transfer in a finite state automation.
	public static DeprivedWriteLatticeElement applyOperation(
			DeprivedWriteLatticeElement element,
			FieldActivityOperation operation) {
		if (element == null) {
			element = TopUnknown;
		}

		switch (element) {
		case TopUnknown:
			switch (operation) {
			case Read:
				return BottomWillBeRead;
			case Write:
				return WillBeWrittenButNotReadThereafter;
			}

		case WillBeWrittenButNotReadThereafter:
			switch (operation) {
			case Read:
				return BottomWillBeRead;
			case Write:
				return WillBeWrittenButNotReadThereafter;
			}
	
		case BottomWillBeRead:
			return BottomWillBeRead;
		}
		throw new OptimizerException(String.format(
				"Bad element (%s) or opreation (%s)", element, operation));
	}
}
