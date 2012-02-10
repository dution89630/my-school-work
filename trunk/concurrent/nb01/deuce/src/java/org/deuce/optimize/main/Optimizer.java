package org.deuce.optimize.main;

import org.deuce.optimize.analyses.fieldactivity.FieldActivityDatabase;
import org.deuce.optimize.analyses.fieldactivity.backwards.DeprivedWriteLatticeElement;
import org.deuce.optimize.analyses.fieldactivity.forwards.VirginReadLatticeElement;
import org.deuce.optimize.analyses.general.SootMethodDatabase;
import org.deuce.optimize.analyses.general.StmtsDatabase;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.analyses.general.UniqueCodePointsAndStmt;
import org.deuce.optimize.analyses.immutables.ImmutableFieldsAccessesDatabase;
import org.deuce.optimize.analyses.lastfieldactivity.LastFieldActivityDatabase;
import org.deuce.optimize.analyses.leakyconstructor.NewlyAllocatedInCtorDatabase;
import org.deuce.optimize.analyses.newlocals.NewLocalsDatabase;
import org.deuce.optimize.analyses.readonlymethod.ReadonlyActivityDatabase;
import org.deuce.optimize.analyses.rescoping.firstfieldactivity.FirstFieldActivityDatabase;
import org.deuce.optimize.analyses.rescoping.firstfieldactivity.InitPoint;
import org.deuce.optimize.analyses.rescoping.firstfieldactivity.MethodInitPointStatus;
import org.deuce.optimize.analyses.rescoping.lastfieldactivity.CommitPoint;
import org.deuce.optimize.analyses.rescoping.lastfieldactivity.CommitPointDatabase;
import org.deuce.optimize.analyses.stable.StableReadsDatabase;
import org.deuce.optimize.analyses.threadescape.ThreadEscapeDatabase;
import org.deuce.optimize.main.settings.OptimizerSettings;
import org.deuce.optimize.main.settings.Settings;
import org.deuce.reflection.FieldOptimizer;

public class Optimizer implements FieldOptimizer {

	private static Optimizer instance = new Optimizer();

	private String className;
	private String methodName;
	private String signature;
	private int bytecodeOffset;

	private final ThreadEscapeDatabase threadEscapeDatabase;
	private final NewlyAllocatedInCtorDatabase newlyAllocatedInCtorDatabase;
	private final NewLocalsDatabase newLocalsDatabase;
	private final ImmutableFieldsAccessesDatabase immutableFieldsAccessesDatabase;
	private final FieldActivityDatabase fieldActivityDatabase;
	private final LastFieldActivityDatabase lastFieldActivityDatabase;
	private final ReadonlyActivityDatabase readonlyActivityDatabase;
	private final FirstFieldActivityDatabase firstFieldActivityDatabase;
	private final CommitPointDatabase commitPointDatabase;
	private final StableReadsDatabase stableReadDatabase;

	public static Optimizer getInstance() {
		return instance;
	}

	private Optimizer() {
		threadEscapeDatabase = ThreadEscapeDatabase.getInstance();
		immutableFieldsAccessesDatabase = ImmutableFieldsAccessesDatabase
				.getInstance();
		newlyAllocatedInCtorDatabase = NewlyAllocatedInCtorDatabase
				.getInstance();
		newLocalsDatabase = NewLocalsDatabase.getInstance();
		fieldActivityDatabase = FieldActivityDatabase.getInstance();
		lastFieldActivityDatabase = LastFieldActivityDatabase.getInstance();
		readonlyActivityDatabase = ReadonlyActivityDatabase.getInstance();
		firstFieldActivityDatabase = FirstFieldActivityDatabase.getInstance();
		commitPointDatabase = CommitPointDatabase.getInstance();
		stableReadDatabase = StableReadsDatabase.getInstance();
	}

	public boolean isFieldNewlyAllocatedObjectLocal() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L13Nl))
			return false;

		if (newLocalsDatabase.contains(new UniqueCodePoint(className,
				methodName, signature, bytecodeOffset)))
			return true;
		return false;
	}

	public boolean isFieldNewlyAllocatedObjectInCtor() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L12Nic))
			return false;

		if (newlyAllocatedInCtorDatabase.contains(new UniqueCodePoint(
				className, methodName, signature, bytecodeOffset)))
			return true;
		return false;
	}

	public boolean isFieldThreadLocal() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L14Tl))
			return false;

		if (threadEscapeDatabase.isEmpty())
			return false;

		boolean escapesThread = threadEscapeDatabase
				.isAccessThreadEscaping(new UniqueCodePoint(className,
						methodName, signature, bytecodeOffset));
		return (!escapesThread);
	}

	public boolean isAccessingImmutableField() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L11Imm))
			return false;

		if (immutableFieldsAccessesDatabase.contains(new UniqueCodePoint(
				className, methodName, signature, bytecodeOffset)))
			return true;
		return false;
	}

	public boolean isPartOfReadOnlyMethod() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L15Rom))
			return false;

		if (readonlyActivityDatabase.isReadonlyActivity(new UniqueCodePoint(
				className, methodName, signature, bytecodeOffset)))
			return true;

		return false;
	}

	public boolean isFieldCurrentlyReadOnly() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L30Ro))
			return false;

		VirginReadLatticeElement virginReadElement = fieldActivityDatabase
				.getVirginReadElement(new UniqueCodePoint(className,
						methodName, signature, bytecodeOffset));
		if (virginReadElement == null) {
			// this statement was not analyzed... probably it is part of the
			// program which cannot be
			// reached anyway, so we didn't analyze it.
			return false;
		}
		if (virginReadElement
				.equals(VirginReadLatticeElement.ReadButNotYetWrittenTo))
			return true;
		return false;
	}

	public boolean isFieldNoLongerReadInTransaction() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L40Wo))
			return false;

		DeprivedWriteLatticeElement deprivedWriteElement = fieldActivityDatabase
				.getDeprivedWriteElement(new UniqueCodePoint(className,
						methodName, signature, bytecodeOffset));
		if (deprivedWriteElement == null) {
			// this statement was not analyzed... probably it is part of the
			// program which cannot be
			// reached anyway, so we didn't analyze it.
			return false;
		}
		if (deprivedWriteElement
				.equals(DeprivedWriteLatticeElement.WillBeWrittenButNotReadThereafter))
			return true;
		return false;
	}

	// public boolean isLastFieldActivity() {
	// if (!Settings.getInstance()
	// .isOptAtLeast(OptimizerSettings.IncludingLFA))
	// return false;

	// disable LFA for the moment.
	// if (lastFieldActivityDatabase.isLastActivity(new UniqueCodePoint(
	// className, methodName, signature, bytecodeOffset)))
	// return true;

	// return false;
	// }

	public boolean doesMethodHaveRecurringInitPoints() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L50Ip))
			return false;

		UniqueCodePoint uniqueCodePoint = new UniqueCodePoint(className,
				methodName, signature);

		if (firstFieldActivityDatabase.getMethodStatus(uniqueCodePoint) == MethodInitPointStatus.HasRecurringInitPoints) {
			return true;
		}
		return false;
	}

	public boolean doesMethodHaveAnyInitPoints() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L50Ip))
			return false;

		UniqueCodePoint uniqueCodePoint = new UniqueCodePoint(className,
				methodName, signature);

		MethodInitPointStatus methodStatus = firstFieldActivityDatabase
				.getMethodStatus(uniqueCodePoint);
		if (methodStatus == MethodInitPointStatus.HasInitPoint
				|| methodStatus == MethodInitPointStatus.HasRecurringInitPoints) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isInitialInitPoint() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L50Ip))
			return false;

		InitPoint initPoint = firstFieldActivityDatabase
				.getInitPoint(new UniqueCodePoint(className, methodName,
						signature, bytecodeOffset));
		return (initPoint == InitPoint.InitialInitPoint);
	}

	@Override
	public boolean isRecurringInitPoint() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L50Ip))
			return false;

		InitPoint initPoint = firstFieldActivityDatabase
				.getInitPoint(new UniqueCodePoint(className, methodName,
						signature, bytecodeOffset));
		return (initPoint == InitPoint.RecurringInitPoint);
	}

	@Override
	public boolean isInitialCommitPoint() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L60Cp))
			return false;

		CommitPoint commitPoint = commitPointDatabase
				.getCommitPoint(new UniqueCodePoint(className, methodName,
						signature, bytecodeOffset));
		return (commitPoint == CommitPoint.LastCommitPoint);
	}

	@Override
	public boolean isRecurringCommitPoint() {
		if (!Settings.getInstance().isOptEnabled(OptimizerSettings.L60Cp))
			return false;

		CommitPoint commitPoint = commitPointDatabase
				.getCommitPoint(new UniqueCodePoint(className, methodName,
						signature, bytecodeOffset));
		return (commitPoint == CommitPoint.RecurringCommitPoint);
	}

	@Override
	public boolean isStableRead() {
		if (!Settings.getInstance().isOptEnabled(
				OptimizerSettings.L70PlusStables))
			return false;

		return stableReadDatabase.contains(new UniqueCodePoint(className,
				methodName, signature, bytecodeOffset));
	}

	public void setCurrentClass(String className) {
		this.className = className;
	}

	public void setCurrentMethod(String methodName) {
		this.methodName = methodName;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public void setCurrentBytecodeOffset(int bytecodeOffset) {
		this.bytecodeOffset = bytecodeOffset;
	}

	public UniqueCodePoint getCurrentUniqueCodePoint() {
		return new UniqueCodePoint(className, methodName, signature,
				bytecodeOffset);
	}

	public UniqueCodePointsAndStmt getCurrentUniqueCodePointsAndStmt() {
		UniqueCodePoint uniqueCodePoint = getCurrentUniqueCodePoint();
		return new UniqueCodePointsAndStmt(uniqueCodePoint, StmtsDatabase
				.getInstance().getStmt(uniqueCodePoint), SootMethodDatabase
				.getInstance().getSootMethod(uniqueCodePoint));
	}

}
