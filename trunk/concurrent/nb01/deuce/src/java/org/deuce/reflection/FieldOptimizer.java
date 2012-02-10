package org.deuce.reflection;

import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.analyses.general.UniqueCodePointsAndStmt;

public interface FieldOptimizer {

	boolean isFieldThreadLocal();

	boolean isAccessingImmutableField();

	boolean isFieldNewlyAllocatedObjectInCtor();

	boolean isFieldNewlyAllocatedObjectLocal();

	boolean isFieldCurrentlyReadOnly();

	boolean isFieldNoLongerReadInTransaction();

	// boolean isLastFieldActivity();

	boolean isPartOfReadOnlyMethod();

	boolean isInitialInitPoint();

	boolean isRecurringInitPoint();

	boolean isInitialCommitPoint();

	boolean isRecurringCommitPoint();

	boolean isStableRead();

	UniqueCodePoint getCurrentUniqueCodePoint();

	UniqueCodePointsAndStmt getCurrentUniqueCodePointsAndStmt();

}
