package org.deuce.optimize.analyses.threadescape;

import java.util.List;

import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.PointsToHelper;
import org.deuce.optimize.utils.SootHierarchyUtils;

import soot.Local;
import soot.PointsToAnalysis;
import soot.RefLikeType;
import soot.Scene;
import soot.SootClass;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.spark.pag.AllocNode;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class ThreadEscapingObjectsAnalysis extends
		ForwardFlowAnalysis<Unit, FlowSet> {
	private final PointsToAnalysis pta;

	// this analysis collects all objects which are either:
	// a. written into static fields,
	// b. written into a java.lang.Runnable object.
	// such objects, and all objects transitively accessible from them through
	// field references, are considered thread-escaping.
	// all other objects are considered thread-local.
	// this analysis only collects the root objects.
	// the propagation only happens in ThreadEscapeSceneTransformer.

	// the flowset is the set of objects that were defined above.
	public ThreadEscapingObjectsAnalysis(ExceptionalUnitGraph graph) {
		super(graph);

		pta = Scene.v().getPointsToAnalysis();

		Logger.println("TEO: Analyzing method: "
				+ (graph).getBody().getMethod().toString());
		doAnalysis();
	}

	@Override
	protected void flowThrough(FlowSet in, Unit unit, FlowSet out) {
		Stmt stmt = (Stmt) unit;

		in.copy(out);

		if (stmt instanceof AssignStmt) {
			Value leftOp = ((AssignStmt) stmt).getLeftOp();
			Value rightOp = ((AssignStmt) stmt).getRightOp();

			// v.f = ...
			if (leftOp instanceof InstanceFieldRef) {

				// v.f = v
				if (rightOp instanceof Local) {
					Local right = (Local) rightOp;
					if (right.getType() instanceof RefLikeType) {
						InstanceFieldRef instanceFieldRef = (InstanceFieldRef) leftOp;
						SootClass declaringClass = instanceFieldRef.getField()
								.getDeclaringClass();
						boolean isRunnable = SootHierarchyUtils
								.implementsInterfaceRecursive(declaringClass,
										"java.lang.Runnable");

						if (isRunnable) {
							// right is written into a Runnable object, therefore it is TEO.
							// the Runnable object is also considered TEO.
							addToOutSet(right, out);
							addToOutSet((Local) instanceFieldRef.getBase(), out);
						}
					}
				}
			}

			// C.f = ...
			else if (leftOp instanceof StaticFieldRef) {
				// C.f = v
				if (rightOp instanceof Local) {
					Local right = (Local) rightOp;
					if (right.getType() instanceof RefLikeType) {
						// right is written to a static class variable, therefore it is TEO
						addToOutSet(right, out);
					}
				}
			}
		}

		// throw v
		else if (stmt instanceof ThrowStmt) {
			Value op = ((ThrowStmt) stmt).getOp();

			if (op instanceof Local) {
				Local v = (Local) op;
				// v is thrown into anywhere, therefore it is conservatively TEO
				addToOutSet(v, out);
			}
		}
	}

	private void addToOutSet(Local right, FlowSet out) {
		List<AllocNode> nodes = PointsToHelper.getNodes(right);
		for (AllocNode allocNode : nodes) {
			out.add(allocNode);
		}
	}

	@Override
	protected void copy(FlowSet source, FlowSet dest) {
		// default copy of source set to destination set
		source.copy(dest);
	}

	@Override
	protected FlowSet entryInitialFlow() {
		// start the method with an empty set of TEO
		return new ArraySparseSet();
	}

	@Override
	protected void merge(FlowSet in1, FlowSet in2, FlowSet out) {
		// merge the sets using union. 
		// we are collection all the TEOs that we discover.
		in1.union(in2, out);
	}

	@Override
	protected FlowSet newInitialFlow() {
		// default TEO set is the empty set
		return new ArraySparseSet();
	}

	public List<AllocNode> getAllEscapingObjects() {
		FlowSet dstFlowSet = newInitialFlow();
		for (Unit unit : graph.getTails()) {
			FlowSet ref = getFlowAfter(unit);
			dstFlowSet.union(ref);
		}
		return dstFlowSet.toList();
	}

}
