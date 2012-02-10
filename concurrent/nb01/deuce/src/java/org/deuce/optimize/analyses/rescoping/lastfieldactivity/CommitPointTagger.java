package org.deuce.optimize.analyses.rescoping.lastfieldactivity;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.deuce.optimize.analyses.general.StmtAndMethodStorage;
import org.deuce.optimize.analyses.general.UniqueCodePoint;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.OptimizerException;
import org.deuce.optimize.utils.scc.SCC;
import org.deuce.optimize.utils.scc.SCCUnitGraph;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public class CommitPointTagger {

	private SootMethod sootMethod;
	private CommitPointDatabase database;
	private CommitPoint seen = CommitPoint.Nothing;

	public void tag(SCCUnitGraph sccUnitGraph, CommitPointAnalysis analyzer) {
		sootMethod = sccUnitGraph.getMethod();
		database = CommitPointDatabase.getInstance();
		for (Iterator<SCC<Unit>> iterator = sccUnitGraph.getSccDirectedGraph()
				.iterator(); iterator.hasNext();) {
			SCC<Unit> scc = iterator.next();
			CommitStatusFlowSet after = analyzer.getFlowAfter(scc);
			CommitStatusFlowSet before = analyzer.getFlowBefore(scc);

			if (before.equals(CommitStatusFlowSet.definitelyNotCommited())) {
				if (after.equals(CommitStatusFlowSet.definitelyCommitted())) {
					// scc is a Last Commit Point
					addToDatabase(scc, CommitPoint.LastCommitPoint);
				} else if (before
						.equals(CommitStatusFlowSet.possiblyCommited())) {
					// scc is a Recurring Commit Point
					addToDatabase(scc, CommitPoint.RecurringCommitPoint);
				}
			}
		}
	}

	private void addToDatabase(SCC<Unit> scc, CommitPoint commitPoint) {
		seen = CommitPoint.values()[Math.max(commitPoint.ordinal(), seen
				.ordinal())];
		if (scc.getNodes().size() != 1) {
			// can't add a commit statement in an SCC that contains more than 1 statement...
			throw new OptimizerException(
					"Impossible that we must instrument a non-singleton SCC.");
		} else {
			Set<Unit> nodes = scc.getNodes();
			for (Unit unit : nodes) {
				Stmt stmt = (Stmt) unit;
				List<UniqueCodePoint> codePoints = UniqueCodePoint
						.generateUniqueCodePointsFor(sootMethod, stmt);
				for (UniqueCodePoint codePoint : codePoints) {
					database.put(codePoint, commitPoint);
					StmtAndMethodStorage.put(codePoint, sootMethod, stmt);
				}

				Logger.println("CPT: " + sootMethod + ": " + unit
						+ " is a commit point of type " + commitPoint,
						sootMethod);
			}
		}
	}

	public CommitPoint getSeen() {
		return seen;
	}
}
