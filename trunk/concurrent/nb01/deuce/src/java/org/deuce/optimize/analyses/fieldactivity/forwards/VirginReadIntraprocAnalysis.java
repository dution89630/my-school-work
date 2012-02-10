package org.deuce.optimize.analyses.fieldactivity.forwards;

import java.util.List;

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
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class VirginReadIntraprocAnalysis extends
		ForwardFlowAnalysis<Unit, VirginReadFlowSet> {
	// this analysis finds for each field whether it is read-only, write-only, or neither.
	// it is intraprocedural and uses points-to information to gain precision.
	// the data is stored at each stmt. later, we extract this data for each statement,
	// and find whether the field is RO, WO, or RW, at that point in the program.
	// the FieldActivityFlowSet is a map between allocation nodes and the activity status.

	//private final boolean isMethodAtomicStarter;
	private final VirginReadInterprocAnalysis interprocAnalysis;
	private final SootMethod sootMethod;
	private final VirginReadFlowSet parameter;

	public VirginReadIntraprocAnalysis(ExceptionalUnitGraph graph,
			VirginReadInterprocAnalysis fieldActivityInterprocAnalysis,
			VirginReadFlowSet parameterValues) {
		super(graph);

		this.sootMethod = graph.getBody().getMethod();
		this.interprocAnalysis = fieldActivityInterprocAnalysis;
		this.parameter = parameterValues;

		Logger.println("FAA: Analyzing method: " + sootMethod.toString(),
				sootMethod);

		//		if (!isMethodAtomicStarter) {
		//			System.out
		//					.println("FAA: Method is not atomic-starter, skipping analysis.");
		//			return;
		//		}

		doAnalysis();
	}

	@Override
	protected void flowThrough(VirginReadFlowSet in, Unit unit,
			VirginReadFlowSet out) {
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
			VirginReadFlowSet returnValue = interprocAnalysis
					.handleMethodInvocation(sootMethod, out, stmt);

			returnValue.mergeInto(out);
		}
	}

	@Override
	protected void copy(VirginReadFlowSet source, VirginReadFlowSet dest) {
		// default copy of source set to destination set
		source.copy(dest);
	}

	@Override
	protected VirginReadFlowSet entryInitialFlow() {
		// start the method with the set of field activities received by the parameter
		VirginReadFlowSet flowSet = new VirginReadFlowSet();
		parameter.copy(flowSet);
		return flowSet;
	}

	@Override
	protected void merge(VirginReadFlowSet in1, VirginReadFlowSet in2,
			VirginReadFlowSet out) {
		// merge the sets using union. 
		// we are collecting all the field activities.
		VirginReadFlowSet merged = VirginReadFlowSet.merge(in1, in2);
		merged.copy(out);
	}

	@Override
	protected VirginReadFlowSet newInitialFlow() {
		// default activity set is the empty set
		return new VirginReadFlowSet();
	}

	VirginReadFlowSet getReturnValue() {
		// if this method is an atomic starter, it doesn't return any information.
		boolean isAtomicStarter = AtomicStartersDatabase.getInstance()
				.isMethodAtomicStarter(sootMethod);
		if (isAtomicStarter) {
			return newInitialFlow();
		} else {
			return getFlowAtEnd();
		}
	}

	public VirginReadFlowSet getFlowAtEnd() {
		VirginReadFlowSet returnValue = newInitialFlow();

		// merge the flowsets of all the method's exit-points
		List<Unit> tails = this.graph.getTails();
		for (Unit unit : tails) {
			VirginReadFlowSet flowAfter = getFlowAfter(unit);
			flowAfter.mergeInto(returnValue);
		}
		return returnValue;
	}

}
