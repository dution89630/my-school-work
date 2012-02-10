package org.deuce.transaction.tl2opt;

import org.deuce.optimize.analyses.general.UniqueCodePointsAndStmt;
import org.deuce.optimize.main.settings.OptimizerSettings;
import org.deuce.optimize.main.settings.Settings;
import org.deuce.reflection.FieldOptimizer;
import org.deuce.transaction.BasicAdvisor;

public class Advisor extends BasicAdvisor {
	public Advisor() {
		AdviceBookkeeper.getBookkeeper().init();
	}

	@Override
	public int visitFieldInsn(FieldOptimizer optimizer) {
		advice = 0;
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L10Opt))
			return super.visitFieldInsn(optimizer);

		// if the field accessed belongs to an object which was allocated in the
		// same transaction, no logging should take place.
		// the object will either die at rollback, or become visible to other
		// threads at commit time.  
		if (optimizer.isFieldNewlyAllocatedObjectInCtor())
			advice = advice | SKIP_NEW_IN_CTOR;

		if (optimizer.isFieldNewlyAllocatedObjectLocal())
			advice = advice | SKIP_NEW_LOCAL;

		// if the field accessed is immutable, its value can never change therefore no logging should take
		// place.
		if (optimizer.isAccessingImmutableField())
			advice = advice | SKIP_IMMUTABLE;

		// field belongs to an object that is not new, but it is not shared 
		// with other threads.
		// Here we can just log it, but skip the validation.
		if (optimizer.isFieldThreadLocal())
			advice = advice | THREAD_LOCAL;

		// field is only being read (not written to) up to this point in this transaction.
		// we can skip looking up this field in the writeset whenever
		// we read it.
		if (optimizer.isFieldCurrentlyReadOnly())
			advice = advice | CURRENTLY_READ_ONLY;

		// field is only being written to in this transaction.
		// when we put it in the writeset, we don't need to update the bloomfilter,
		// because the bloomfilter is only used for lookups.
		if (optimizer.isFieldNoLongerReadInTransaction())
			advice = advice | WRITE_ONLY_IN_TRANSACTION;

		// this statement is part of a read-only method.
		// we don't need to add anything to the read-set, because it will never be looked up.
		if (optimizer.isPartOfReadOnlyMethod())
			advice = advice | READ_ONLY_METHOD;

		// no statement that follows the current statement needs to be instrumented.
		// we can commit right there.
//		if (optimizer.isLastFieldActivity())
//			advice = advice | LAST_FIELD_ACTIVITY;

		if (optimizer.isInitialInitPoint())
			advice = advice | INITIAL_INIT_POINT;

		if (optimizer.isRecurringInitPoint())
			advice = advice | RECURRING_INIT_POINT;

		if (optimizer.isInitialCommitPoint())
			advice = advice | INITIAL_COMMIT_POINT;

		if (optimizer.isRecurringCommitPoint())
			advice = advice | RECURRING_COMMIT_POINT;

		if (optimizer.isStableRead())
			advice = advice | STABLE_READ;

		UniqueCodePointsAndStmt codePoint = optimizer
				.getCurrentUniqueCodePointsAndStmt();
		AdviceBookkeeper.getBookkeeper().accountFor(advice, codePoint);

		return super.visitFieldInsn(optimizer);
	}

	@Override
	public String adviceGiven() {
		return AdviceBookkeeper.getBookkeeper().toString();
	}
}
