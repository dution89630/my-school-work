package org.deuce.optimize.utils;

import java.util.List;

import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.analyses.immutables.ImmutableFieldsAccessesDatabase;
import org.deuce.optimize.analyses.leakyconstructor.NewlyAllocatedInCtorDatabase;
import org.deuce.optimize.analyses.newlocals.NewLocalsDatabase;
import org.deuce.optimize.analyses.puremethod.PureMethodsDatabase;

import soot.SootMethod;
import soot.jimple.Stmt;

public class OptimizerUtils {

	private final ImmutableFieldsAccessesDatabase immutableFieldsAccessesDatabase;
	private final NewlyAllocatedInCtorDatabase newlyAllocatedInCtorDatabase;
	private final NewLocalsDatabase newLocalsDatabase;
	private final PureMethodsDatabase pureMethodsDatabase;

	public OptimizerUtils() {
		this.immutableFieldsAccessesDatabase = ImmutableFieldsAccessesDatabase
				.getInstance();
		this.newlyAllocatedInCtorDatabase = NewlyAllocatedInCtorDatabase
				.getInstance();
		this.newLocalsDatabase = NewLocalsDatabase.getInstance();
		this.pureMethodsDatabase = PureMethodsDatabase.getInstance();
	}

	public boolean isStmtMustBeInstrumented(Stmt stmt, SootMethod sootMethod) {
		if (stmt.containsFieldRef() || stmt.containsArrayRef()) {
			// stmt is a field access or an array access
			return mustFieldStmtBeInstrumented(stmt, sootMethod);
		} else if (stmt.containsInvokeExpr()) {
			// stmt is a method invocation 
			return mustInvocationStmtBeInstrumented(stmt, sootMethod);
		} else {
			return false;
		}
	}

	public boolean mustFieldStmtBeInstrumented(Stmt stmt, SootMethod sootMethod) {
		List<UniqueCodePoint> uniqueCodePoints = UniqueCodePoint
				.generateUniqueCodePointsFor(sootMethod, stmt);
		for (UniqueCodePoint uniqueCodePoint : uniqueCodePoints) {
			boolean immutbable = immutableFieldsAccessesDatabase
					.contains(uniqueCodePoint);
			boolean newLocal = newLocalsDatabase.contains(uniqueCodePoint);
			boolean newInCtor = newlyAllocatedInCtorDatabase
					.contains(uniqueCodePoint);
			if (!immutbable && !newLocal && !newInCtor)
				return true;
		}
		// if got here: all codepoints of the received stmt were found to 
		// be either immutable, new local or new in construtor.
		// therefore the stmt doesn't have to be instrumented.
		return false;
	}

	public boolean mustInvocationStmtBeInstrumented(Stmt stmt,
			SootMethod sootMethod) {
		List<SootMethod> callees = MethodUtils.getCalleesOf(sootMethod, stmt);
		for (SootMethod callee : callees) {
			if (!pureMethodsDatabase.isMethodPure(callee)) {
				// stmt is followed by an invocation to an impure method
				return true;
			}
		}
		// all callees are pure
		return false;
	}
}
