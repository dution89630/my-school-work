package org.deuce.optimize.analyses.pre.sp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.deuce.optimize.analyses.pre.SimpleLoop;

import soot.Body;
import soot.BodyTransformer;
import soot.EquivalentValue;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.ConcreteRef;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.toolkits.pointer.PASideEffectTester;
import soot.shimple.toolkits.scalar.SEvaluator.BottomConstant;
import soot.shimple.toolkits.scalar.SEvaluator.TopConstant;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;

public class ScalarPromotion extends BodyTransformer {

	private BlockGraph blockGraph;
	private ArrayList<SimpleLoop> simpleLoops;
	private PASideEffectTester sideEffect;
	private Promoter promoter;

	@Override
	protected void internalTransform(Body body, String phaseName, Map options) {
		// look for assignment into fields or arrays
		// o.f = ..., or a[i] = ...
		// such that:
		// 1. o is not written to
		// 2. a and i are not written to
		// 3. a[i] is not written to indirectly (e.g. by writing into a[j])

		blockGraph = new BriefBlockGraph(body);
		promoter = new Promoter(body);
		sideEffect = new PASideEffectTester();
		sideEffect.newMethod(body.getMethod());

		findSimpleLoops();
		for (SimpleLoop simpleLoop : simpleLoops) {
			processSimpleLoop(simpleLoop);
		}
		processWholeMethod(body.getMethod(), body.getUnits());
	}

	private void findSimpleLoops() {
		simpleLoops = new ArrayList<SimpleLoop>();
		for (Block body : blockGraph.getBlocks()) {
			List<Block> bodySuccs = body.getSuccs();
			if (bodySuccs.size() != 1) {
				// the body must have just one successor
				continue;
			}
			Block condition = bodySuccs.get(0);
			List<Block> conditionSuccs = condition.getSuccs();
			if (conditionSuccs.size() != 2) {
				// the condition must have two successors
				continue;
			}
			Block conditionSucc0 = conditionSuccs.get(0);
			Block conditionSucc1 = conditionSuccs.get(1);
			Block rest = null;
			if (conditionSucc0 == body)
				rest = conditionSucc1;
			else if (conditionSucc1 == body)
				rest = conditionSucc0;
			else {
				// the condition must branch into body
				continue;
			}
			if (body.getPreds().size() != 1) {
				// the body must have just one predecessor (the condition)
				continue;
			}
			SimpleLoop simpleLoop = new SimpleLoop(body, condition, rest);
			simpleLoops.add(simpleLoop);
		}

	}

	private void processWholeMethod(SootMethod sootMethod,
			PatchingChain<Unit> units) {
		LinkedHashSet<EquivalentValue> expressions = collectWritableRefs(units);
		ArrayList<Promotable> promotables = findPromotableValues(units,
				expressions, false);
		if (promotables.size() != 0) {
			System.out.println("!!!Promotables in method "
					+ sootMethod.toString() + ":" + promotables);
		}
	}

	private void processSimpleLoop(SimpleLoop simpleLoop) {
		// collect expressions that are written to
		List<Unit> loopStatements = simpleLoop.getLoopStatements();
		LinkedHashSet<EquivalentValue> expressions = collectWritableRefs(loopStatements);
		ArrayList<Promotable> promotables = findPromotableValues(
				loopStatements, expressions, true);
		if (promotables.size() != 0) {
			promoter.promote(simpleLoop, loopStatements, promotables);
		}
	}

	private ArrayList<Promotable> findPromotableValues(
			Collection<Unit> loopStatements,
			LinkedHashSet<EquivalentValue> expressions, boolean isInLoop) {
		ArrayList<Promotable> promotables = new ArrayList<Promotable>();
		for (EquivalentValue equivValue : expressions) {
			ConcreteRef concreteRef = (ConcreteRef) equivValue
					.getDeepestValue();
			Promotable promotable = checkIfPromotable(loopStatements,
					concreteRef);
			if (promotable != null) {
				if (isInLoop
						|| assignedMoreThanOnce(loopStatements, concreteRef)) {
					promotables.add(promotable);
				}
			}
		}
		return promotables;
	}

	private boolean assignedMoreThanOnce(Collection<Unit> loopStatements,
			ConcreteRef concreteRef) {
		int assignedCount = 0;
		for (Unit unit : loopStatements) {
			if (unit instanceof AssignStmt) {
				Value leftOp = ((AssignStmt) unit).getLeftOp();
				if (leftOp.equivTo(concreteRef))
					++assignedCount;
			}
		}
		return assignedCount > 1;
	}

	private LinkedHashSet<EquivalentValue> collectWritableRefs(
			Collection<Unit> loopStatements) {
		LinkedHashSet<EquivalentValue> writableRefs = new LinkedHashSet<EquivalentValue>();
		for (Unit unit : loopStatements) {
			if (unit instanceof AssignStmt) {
				Value leftOp = ((AssignStmt) unit).getLeftOp();
				if (leftOp instanceof ConcreteRef) {
					writableRefs.add(new EquivalentValue(leftOp));
				}
			}
		}
		return writableRefs;
	}

	private Promotable checkIfPromotable(Collection<Unit> loopStatements,
			ConcreteRef concreteRef) {
		if (concreteRef instanceof ArrayRef)
			return checkIfPromotableArrayRef(loopStatements,
					(ArrayRef) concreteRef);
		else if (concreteRef instanceof StaticFieldRef)
			return checkIfPromotableStaticFieldRef(loopStatements,
					(StaticFieldRef) concreteRef);
		else if (concreteRef instanceof InstanceFieldRef)
			return checkIfPromotableInstanceFieldRef(loopStatements,
					(InstanceFieldRef) concreteRef);
		else
			throw new RuntimeException(String.format(
					"ConcreteRef '%s' is neither FieldRef not ArrayRef",
					concreteRef));
	}

	private Promotable checkIfPromotableStaticFieldRef(
			Collection<Unit> loopStatements, StaticFieldRef fieldRef) {
		// assignments of the form C.f = ...
		// are promotable if:
		// 1. all units which modify C.f are an assignment statement C.f = ...

		for (Unit unit : loopStatements) {
			if (sideEffect.unitCanWriteTo(unit, fieldRef)
					&& !fieldRefWriteIsExact(unit, fieldRef))
				return null;
		}
		return new Promotable(fieldRef);
	}

	private Promotable checkIfPromotableInstanceFieldRef(
			Collection<Unit> loopStatements, InstanceFieldRef fieldRef) {
		// assignments of the form o.f = ...
		// are promotable if:
		// 1. o is not written to
		// 2. all units which modify o.f is an assignment statement o.f = ...

		Value o = fieldRef.getBase();
		for (Unit unit : loopStatements) {
			if (unit instanceof IdentityStmt)
				continue;
			if (sideEffect.unitCanWriteTo(unit, o))
				return null;
			if (sideEffect.unitCanWriteTo(unit, fieldRef)
					&& !fieldRefWriteIsExact(unit, fieldRef))
				return null;
		}
		return new Promotable(fieldRef);
	}

	private Promotable checkIfPromotableArrayRef(
			Collection<Unit> loopStatements, ArrayRef arrayRef) {
		// assignments of the form a[i] = ...
		// are promotable if:
		// 1. a is not written to
		// 2. i is not written to
		// 2. all units which modify a[i] are an assignment statement a[i] = ...

		Value a = arrayRef.getBase();
		Value i = arrayRef.getIndex();

		for (Unit unit : loopStatements) {
			if (sideEffect.unitCanWriteTo(unit, a))
				return null;
			if (sideEffect.unitCanWriteTo(unit, arrayRef)
					&& !arrayRefWriteIsExact(unit, arrayRef)
					&& getConstantValueWrittenToi(unit, i) == TopConstant.v())
				return null;
		}
		Constant indexConstantValue = checkIfArrayIndexIsConstant(
				loopStatements, arrayRef);
		if (indexConstantValue != TopConstant.v())
			return new Promotable(arrayRef, indexConstantValue);
		return null;
	}

	// returns BottomConstant if i is not assigned
	// returns IntConstant if i is assigned a single constant before
	// accessing a[i]
	// return TopConstant otherwise
	private Constant checkIfArrayIndexIsConstant(
			Collection<Unit> loopStatements, ArrayRef arrayRef) {
		Value a = arrayRef.getBase();
		Value i = arrayRef.getIndex();

		Constant bottom = BottomConstant.v();
		Constant currentiValue = bottom;
		boolean iValueWasAlreadyRead = false;
		for (Unit unit : loopStatements) {
			boolean iWasRead = sideEffect.unitCanReadFrom(unit, i);
			boolean iWasWritten = sideEffect.unitCanWriteTo(unit, i);
			Constant valueWrittenToi = getConstantValueWrittenToi(unit, i);
			if (iWasRead) {
				iValueWasAlreadyRead = true;
			}
			if (iWasWritten && iValueWasAlreadyRead) {
				// i was read and then re-written
				return TopConstant.v();
			}
			if (iWasWritten && valueWrittenToi == null) {
				// i was written a non-const or undetermined value
				return TopConstant.v();
			}
			if (iWasWritten && currentiValue != bottom
					&& valueWrittenToi != currentiValue) {
				// i was written a different constant than before
				return TopConstant.v();
			}
			if (iWasWritten) {
				currentiValue = valueWrittenToi;
			}

		}
		return currentiValue;
	}

	private Constant getConstantValueWrittenToi(Unit unit, Value i) {
		if (unit instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) unit;
			Value leftOp = assignStmt.getLeftOp();
			Value rightOp = assignStmt.getRightOp();
			if (leftOp.equivTo(i))
				if (rightOp instanceof Constant)
					return (Constant) rightOp;
				else
					return TopConstant.v();
		}
		return null;
	}

	private boolean arrayRefWriteIsExact(Unit unit, ArrayRef arrayRef) {
		// an array ref write is exact, if it is a statement of the
		// form: a[i] = ...
		// (non-exact array ref writes are method invocations that
		// modify the array, or assignments to a[j] where j
		// might have the same runtime value as i,
		// or assignments to b[i] where b might have the same
		// runtime value as a
		if (unit instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) unit;
			Value unitLeftOp = assignStmt.getLeftOp();
			if (unitLeftOp instanceof ArrayRef) {
				ArrayRef unitArrayRef = assignStmt.getArrayRef();
				if (unitArrayRef.equivTo(arrayRef))
					return true;
			}
		}
		return false;
	}

	private boolean fieldRefWriteIsExact(Unit unit, FieldRef fieldRef) {
		// a static field write is exact, if it is a statement
		// of the form: C.f = ...
		// an instance field write is exact, if it is a statement
		// of the form: o.f = ...
		if (unit instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) unit;
			Value unitLeftOp = assignStmt.getLeftOp();
			if (unitLeftOp instanceof FieldRef) {
				FieldRef unitFieldRef = assignStmt.getFieldRef();
				if (unitFieldRef.equivTo(fieldRef))
					return true;
			}
		}
		return false;
	}

}
