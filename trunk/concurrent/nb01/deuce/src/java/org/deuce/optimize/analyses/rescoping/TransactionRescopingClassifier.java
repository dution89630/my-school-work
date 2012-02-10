package org.deuce.optimize.analyses.rescoping;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.deuce.optimize.utils.OptimizerUtils;
import org.deuce.optimize.utils.scc.SCC;
import org.deuce.optimize.utils.scc.SCCUnitGraph;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.DirectedGraph;

public class TransactionRescopingClassifier {

	private final SCCUnitGraph sccUnitGraph;
	private final LinkedHashSet<SCC<Unit>> mustInstrument;
	private final LinkedHashSet<SCC<Unit>> canInjectHere;
	private final DirectedGraph<SCC<Unit>> sccDirectedGraph;
	private final OptimizerUtils optimizerUtils;

	public TransactionRescopingClassifier(SCCUnitGraph sccUnitGraph) {
		this.sccUnitGraph = sccUnitGraph;
		this.sccDirectedGraph = sccUnitGraph.getSccDirectedGraph();
		this.mustInstrument = new LinkedHashSet<SCC<Unit>>();
		this.canInjectHere = new LinkedHashSet<SCC<Unit>>();
		this.optimizerUtils = new OptimizerUtils();
	}

	public LinkedHashSet<SCC<Unit>> getMustInstrument() {
		return mustInstrument;
	}

	public LinkedHashSet<SCC<Unit>> getCanInjectHere() {
		return canInjectHere;
	}

	public void classify(Direction propagationDirection) {
		// collect all the SCCs that must be instrumented. Those are the SCCs that contain at least
		// one statement that is an invocation, array access or field access.
		initializeMustInstrumentSet();

		// collect all the SCCs for which we can inject an init or commit statement. Those are the SCCs consisting
		// of a single statement, which also has (at least one) bytecode tag. 
		initializeCanInjectHereSet();

		// for every "must instrument" unit that is not "can inject", must mark all its predecessors, or successors, 
		// as "must instrument".
		// for init point, we propagate backwards until every path from the root 
		// "can inject" its first "must instrument".
		// for commit point, we propagate forwards until every path from any tail
		// "can inject" its first "must instrument".
		propagate(propagationDirection);
	}

	private void propagate(Direction propagationDirection) {
		while (true) {
			Set<SCC<Unit>> mustInstrumentThoseToo = new LinkedHashSet<SCC<Unit>>();
			for (SCC<Unit> scc : mustInstrument) {
				if (!canInjectHere.contains(scc)) {
					List<SCC<Unit>> neighborsOf = null;
					if (propagationDirection == Direction.Backwards)
						neighborsOf = sccDirectedGraph.getPredsOf(scc);
					else if (propagationDirection == Direction.Forwards)
						neighborsOf = sccDirectedGraph.getSuccsOf(scc);

					for (SCC<Unit> neighbot : neighborsOf) {
						if (!mustInstrument.contains(neighbot)) {
							mustInstrumentThoseToo.add(neighbot);
						}
					}
				}
			}
			if (mustInstrumentThoseToo.isEmpty())
				return;
			for (SCC<Unit> scc : mustInstrumentThoseToo) {
				mustInstrument.add(scc);
			}
		}
	}

	private void initializeCanInjectHereSet() {
		for (Iterator<SCC<Unit>> iterator = sccDirectedGraph.iterator(); iterator
				.hasNext();) {
			SCC<Unit> scc = iterator.next();
			if (canInjectAtThisScc(scc)) {
				this.canInjectHere.add(scc);
			}
		}
	}

	private boolean canInjectAtThisScc(SCC<Unit> scc) {
		if (scc.getNodes().size() != 1) {
			// can't inject any statement in an SCC that contains more than 1 statement...
			return false;
		} else {
			Set<Unit> nodes = scc.getNodes();
			for (Unit unit : nodes) {
				Stmt stmt = (Stmt) unit;
				// inject only at array refs, field refs or method invocation.
				// this is only a convenience, because Deuce only transforms those commands. 
				// (in DuplicateMethod)
				if (stmt.containsArrayRef() || stmt.containsFieldRef()
						|| stmt.containsInvokeExpr()) {
					List<Tag> tags = unit.getTags();
					for (Tag tag : tags) {
						if (tag instanceof BytecodeOffsetTag) {
							// found at least one bytecode offset tag.
							// we can init immediately after this scc by adding our command
							// after this (or, the last) tag.
							// we can commit immediately after this scc as well.
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void initializeMustInstrumentSet() {
		for (Iterator<SCC<Unit>> iterator = sccDirectedGraph.iterator(); iterator
				.hasNext();) {
			SCC<Unit> scc = iterator.next();
			if (isSccMustBeInstrumented(scc)) {
				this.mustInstrument.add(scc);
			}
		}
	}

	private boolean isSccMustBeInstrumented(SCC<Unit> scc) {
		Set<Unit> nodes = scc.getNodes();
		for (Unit unit : nodes) {
			Stmt stmt = (Stmt) unit;
			SootMethod method = sccUnitGraph.getMethod();
			if (optimizerUtils.isStmtMustBeInstrumented(stmt, method))
				return true;
		}
		return false;
	}

}
