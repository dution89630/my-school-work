package org.deuce.optimize.analyses.fieldactivity.backwards;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.deuce.optimize.analyses.atomicstarter.AtomicStartersDatabase;
import org.deuce.optimize.analyses.fieldactivity.AllocNodeAndField;
import org.deuce.optimize.analyses.fieldactivity.FieldActivityOperation;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.PointsToHelper;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.spark.pag.AllocNode;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;

public class DeprivedWriteIntraprocAnalysis extends
		BackwardFlowAnalysis<Unit, DeprivedWriteFlowSet> {
	// this analysis finds for each field whether it is read-only, write-only, or neither.
	// it is intraprocedural and uses points-to information to gain precision.
	// the data is stored at each stmt. later, we extract this data for each statement,
	// and find whether the field is RO, WO, or RW, at that point in the program.
	// the FieldActivityFlowSet is a map between allocation nodes and the activity status.

	//private final boolean isMethodAtomicStarter;
	private final DeprivedWriteInterprocAnalysis interprocAnalysis;
	private final SootMethod sootMethod;
	private final DeprivedWriteFlowSet parameter;
	private LinkedHashMap<Stmt, Set<Stmt>> tailToReachableStmts;

	public DeprivedWriteIntraprocAnalysis(ExceptionalUnitGraph graph,
			DeprivedWriteInterprocAnalysis fieldActivityInterprocAnalysis,
			DeprivedWriteFlowSet parameterValues) {
		super(graph);

		this.sootMethod = graph.getBody().getMethod();
		this.interprocAnalysis = fieldActivityInterprocAnalysis;
		this.parameter = parameterValues;

		Logger.println("FAA: Analyzing method: " + sootMethod.toString(),
				sootMethod);

		doAnalysis();
	}

	@Override
	protected void flowThrough(DeprivedWriteFlowSet in, Unit unit,
			DeprivedWriteFlowSet out) {
		Stmt stmt = (Stmt) unit;

		in.copy(out);

		boolean isApplicationClass = this.sootMethod.getDeclaringClass()
				.isApplicationClass();
		if (stmt instanceof AssignStmt) {
			Value leftOp = ((AssignStmt) stmt).getLeftOp();
			Value rightOp = ((AssignStmt) stmt).getRightOp();

			// v.f = ...
			if (leftOp instanceof InstanceFieldRef) {
				InstanceFieldRef instanceFieldRef = (InstanceFieldRef) leftOp;
				Local base = (Local) instanceFieldRef.getBase();
				SootField field = instanceFieldRef.getField();
				List<AllocNode> nodes = PointsToHelper.getNodes(base);
				for (AllocNode allocNode : nodes) {
					AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
							allocNode, field.getName());
					out.applyAndMerge(allocNodeAndField,
							FieldActivityOperation.Write, isApplicationClass);
				}
			}

			// v[i] = ...
			if (leftOp instanceof ArrayRef) {
				ArrayRef arrayRef = (ArrayRef) leftOp;
				Local base = (Local) arrayRef.getBase();
				List<AllocNode> nodes = PointsToHelper.getNodes(base);
				for (AllocNode allocNode : nodes) {
					AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
							allocNode, AllocNodeAndField.ARRAY_ELEMENTS);
					out.applyAndMerge(allocNodeAndField,
							FieldActivityOperation.Write, isApplicationClass);
				}
			}

			// C.f = ...
			if (leftOp instanceof StaticFieldRef) {
				StaticFieldRef staticFieldRef = (StaticFieldRef) leftOp;
				Type type = staticFieldRef.getType();
				SootField field = staticFieldRef.getField();
				AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
						type, field.getName());
				out.applyAndMerge(allocNodeAndField,
						FieldActivityOperation.Write, isApplicationClass);
			}

			// ... = v.f
			if (rightOp instanceof InstanceFieldRef) {
				InstanceFieldRef instanceFieldRef = (InstanceFieldRef) rightOp;
				Local base = (Local) instanceFieldRef.getBase();
				SootField field = instanceFieldRef.getField();
				List<AllocNode> nodes = PointsToHelper.getNodes(base);
				for (AllocNode allocNode : nodes) {
					AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
							allocNode, field.getName());
					out.applyAndMerge(allocNodeAndField,
							FieldActivityOperation.Read, isApplicationClass);
				}
			}

			// ... = v[i]
			if (rightOp instanceof ArrayRef) {
				ArrayRef arrayRef = (ArrayRef) rightOp;
				Local base = (Local) arrayRef.getBase();
				List<AllocNode> nodes = PointsToHelper.getNodes(base);
				for (AllocNode allocNode : nodes) {
					AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
							allocNode, AllocNodeAndField.ARRAY_ELEMENTS);
					out.applyAndMerge(allocNodeAndField,
							FieldActivityOperation.Read, isApplicationClass);
				}
			}

			// ... = C.f
			if (rightOp instanceof StaticFieldRef) {
				StaticFieldRef staticFieldRef = (StaticFieldRef) rightOp;
				Type type = staticFieldRef.getType();
				SootField field = staticFieldRef.getField();
				AllocNodeAndField allocNodeAndField = new AllocNodeAndField(
						type, field.getName());
				out.applyAndMerge(allocNodeAndField,
						FieldActivityOperation.Read, isApplicationClass);
			}
		}
		if (stmt.containsInvokeExpr()) {

			// handle method invocation: flow parameters into method call, 
			// and flow return value from method call
			DeprivedWriteFlowSet returnValue = interprocAnalysis
					.handleMethodInvocation(sootMethod, out, stmt);

			returnValue.mergeInto(out);
		}
	}

	@Override
	protected void copy(DeprivedWriteFlowSet source, DeprivedWriteFlowSet dest) {
		// default copy of source set to destination set
		source.copy(dest);
	}

	@Override
	protected DeprivedWriteFlowSet entryInitialFlow() {
		// start the method with the set of field activities received by the parameter
		DeprivedWriteFlowSet flowSet = new DeprivedWriteFlowSet();
		parameter.copy(flowSet);
		return flowSet;
	}

	@Override
	protected void merge(DeprivedWriteFlowSet in1, DeprivedWriteFlowSet in2,
			DeprivedWriteFlowSet out) {
		// merge the sets using union. 
		// we are collecting all the field activities.
		DeprivedWriteFlowSet merged = DeprivedWriteFlowSet.merge(in1, in2);
		merged.copy(out);
	}

	@Override
	protected DeprivedWriteFlowSet newInitialFlow() {
		// default activity set is the empty set
		return new DeprivedWriteFlowSet();
	}

	DeprivedWriteFlowSet getReturnValue() {
		// if this method is an atomic starter, it doesn't return any information.
		boolean isAtomicStarter = AtomicStartersDatabase.getInstance()
				.isMethodAtomicStarter(sootMethod);
		if (isAtomicStarter) {
			return newInitialFlow();
		} else {
			return getFlowAtHeads();
		}
	}

	public DeprivedWriteFlowSet getFlowAtHeads() {
		DeprivedWriteFlowSet returnValue = newInitialFlow();

		// merge the flowsets of all the method's entry-points
		List<Unit> heads = this.graph.getHeads();
		for (Unit unit : heads) {
			DeprivedWriteFlowSet flowAfter = getFlowBefore(unit);
			flowAfter.mergeInto(returnValue);
		}
		return returnValue;
	}
}
