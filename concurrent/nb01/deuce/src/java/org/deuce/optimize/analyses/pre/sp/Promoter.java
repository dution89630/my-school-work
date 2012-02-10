package org.deuce.optimize.analyses.pre.sp;

import java.util.ArrayList;
import java.util.List;

import org.deuce.optimize.analyses.pre.SimpleLoop;
import org.deuce.optimize.analyses.pre.lcm.Manipulations;

import soot.Body;
import soot.G;
import soot.Local;
import soot.PatchingChain;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.ConcreteRef;
import soot.jimple.Constant;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.scalar.LocalCreation;
import soot.shimple.toolkits.scalar.SEvaluator.BottomConstant;
import soot.toolkits.graph.Block;

public class Promoter {
	private static final String PREFIX = "$sp";
	private final Manipulations manipulations;
	private final LocalCreation localCreation;
	private final Body body;

	public Promoter(Body body) {
		this.body = body;
		this.manipulations = new Manipulations(body);
		this.localCreation = new LocalCreation(body.getLocals(), PREFIX);
	}

	void promote(SimpleLoop simpleLoop, List<Unit> loopStatements,
			ArrayList<Promotable> promotables) {
		if (promotables.size() == 0)
			return;
		Unit preheader = preparePreheader(simpleLoop);
		Unit trailer = prepareTrailer(simpleLoop);
		for (Promotable promotable : promotables) {
			promote(promotable.getConcreteRef(), promotable.getIntConstant(),
					loopStatements, preheader, trailer);
		}
	}

	private Unit preparePreheader(SimpleLoop simpleLoop) {
		// create a preheader
		NopStmt preheaderHead = Jimple.v().newNopStmt();
		PatchingChain<Unit> bodyUnits = body.getUnits();
		Unit lastUnit = manipulations.getAnyReturnUnit();
		bodyUnits.insertAfter(preheaderHead, lastUnit);

		// redirect all condition's external predecessors into the preheader
		Block condition = simpleLoop.getCondition();
		Block loopBody = simpleLoop.getBody();
		Unit conditionHead = condition.getHead();
		List<Block> predBlocks = condition.getPreds();
		for (Block predBlock : predBlocks) {
			if (predBlock == loopBody)
				continue;
			Unit predTail = predBlock.getTail();
			manipulations.redirectExitToNewTarget(conditionHead, preheaderHead,
					predTail);
		}

		// redirect preheader's exit into the condition
		manipulations.insertGotoAfter(preheaderHead, conditionHead);
		return preheaderHead;
	}

	private Unit prepareTrailer(SimpleLoop simpleLoop) {
		// create a tailer
		NopStmt trailerHead = Jimple.v().newNopStmt();
		PatchingChain<Unit> bodyUnits = body.getUnits();
		Unit lastUnit = manipulations.getAnyReturnUnit();
		bodyUnits.insertAfter(trailerHead, lastUnit);

		// redirect condition's external successor into the trailer
		Block condition = simpleLoop.getCondition();
		Block body = simpleLoop.getBody();
		Unit conditionTail = condition.getTail();
		List<Block> succBlocks = condition.getSuccs();
		for (Block succBlock : succBlocks) {
			if (succBlock == body)
				continue;
			Unit succHead = succBlock.getHead();
			manipulations.redirectExitToNewTarget(succHead, trailerHead,
					conditionTail);

			// redirect trailer's exit into the successor's head
			manipulations.insertGotoAfter(trailerHead, succHead);
			return trailerHead;
		}
		throw new RuntimeException(
				"Condition only leads to body. Loop has no exit?");
	}

	private void promote(ConcreteRef concreteRef, Constant constant,
			List<Unit> loopStatements, Unit preheader, Unit trailer) {
		// create a new helper
		Local helper = localCreation.newLocal(concreteRef.getType());
		G.v().out.println("SP: " + body.getMethod().toString()
				+ ": replacing assignments to " + concreteRef.toString()
				+ " with a local " + helper.toString());

		populatePreheader(concreteRef, preheader, helper);
		populatePreheaderWithIndex(concreteRef, constant, preheader);

		populateTrailer(concreteRef, trailer, helper);

		replaceOccurences(concreteRef, loopStatements, helper);
	}

	private void populatePreheaderWithIndex(ConcreteRef concreteRef,
			Constant constant, Unit preheader) {
		// in preheader: put i = <const>
		if (constant != null && constant != BottomConstant.v()) {
			Value indexValue = ((ArrayRef) concreteRef).getIndex();

			Value insertValue1 = Jimple.cloneIfNecessary(constant);
			Unit assignmentToHelper = Jimple.v().newAssignStmt(indexValue,
					insertValue1);
			body.getUnits().insertAfter(assignmentToHelper, preheader);
		}
	}

	private void populatePreheader(ConcreteRef concreteRef, Unit preheader,
			Local helper) {
		// in preheader: put $sp0 = a[i] ($sp0 = o.f)
		Value insertValue1 = Jimple.cloneIfNecessary(concreteRef);
		Unit assignmentToHelper = Jimple.v()
				.newAssignStmt(helper, insertValue1);
		body.getUnits().insertAfter(assignmentToHelper, preheader);
	}

	private void populateTrailer(ConcreteRef concreteRef, Unit trailer,
			Local helper) {
		// in trailer: put a[i] = $sp0 (o.f = $sp0)
		Value insertValue2 = Jimple.cloneIfNecessary(concreteRef);
		Unit assignmentFromHelper = Jimple.v().newAssignStmt(insertValue2,
				helper);
		body.getUnits().insertAfter(assignmentFromHelper, trailer);
	}

	private void replaceOccurences(ConcreteRef concreteRef,
			List<Unit> loopStatements, Local helper) {
		// replace all occurences of a[i] (o.f) with $sp0
		for (Unit unit : loopStatements) {
			Stmt stmt = (Stmt) unit;
			ValueBox refBox = null;
			if (stmt.containsArrayRef()) {
				refBox = stmt.getArrayRefBox();
			} else if (stmt.containsFieldRef()) {
				refBox = stmt.getFieldRefBox();
			} else {
				continue;
			}

			if (refBox.getValue().equivTo(concreteRef)) {
				refBox.setValue(helper);
			}
		}
	}

}
