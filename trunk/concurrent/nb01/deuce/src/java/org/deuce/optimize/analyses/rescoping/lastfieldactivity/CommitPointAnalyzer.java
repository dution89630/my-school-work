package org.deuce.optimize.analyses.rescoping.lastfieldactivity;

import java.util.List;

import org.deuce.optimize.analyses.atomicstarter.AtomicStartersDatabase;
import org.deuce.optimize.analyses.rescoping.Direction;
import org.deuce.optimize.analyses.rescoping.TransactionRescopingClassifier;
import org.deuce.optimize.utils.Logger;
import org.deuce.optimize.utils.scc.SCC;
import org.deuce.optimize.utils.scc.SCCBuilder;
import org.deuce.optimize.utils.scc.SCCUnitGraph;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class CommitPointAnalyzer {

	private final CommitPointDatabase commitPointDatabase;
	private final AtomicStartersDatabase atomicStartersDatabase;

	public CommitPointAnalyzer() {
		commitPointDatabase = CommitPointDatabase.getInstance();
		atomicStartersDatabase = AtomicStartersDatabase.getInstance();
	}

	public void analyzeMethod(SootMethod method) {
		commitPointDatabase.put(method, MethodCommitPointStatus.NotOptimizable);

		if (!method.hasActiveBody()) {
			return;
		}
		if (!atomicStartersDatabase.isMethodAtomicStarter(method)) {
			return;
		}

		SCCUnitGraph sccUnitGraph = buildSccGraph(method);

		TransactionRescopingClassifier classifier = new TransactionRescopingClassifier(
				sccUnitGraph);
		classifier.classify(Direction.Forwards);

		if (!optimizationViable(sccUnitGraph, classifier)) {
			return;
		}

		internalAnalyzeMethod(method, sccUnitGraph, classifier);
	}

	private void internalAnalyzeMethod(SootMethod method,
			SCCUnitGraph sccUnitGraph, TransactionRescopingClassifier classifier) {
		Logger.println("CPA: Analyzing method: " + method.toString(), method);

		CommitPointAnalysis analysis = new CommitPointAnalysis(sccUnitGraph,
				classifier);

		CommitPointTagger tagger = new CommitPointTagger();
		tagger.tag(sccUnitGraph, analysis);
		CommitPoint seen = tagger.getSeen();
		MethodCommitPointStatus mos = MethodCommitPointStatus.NotOptimizable;
		if (seen == CommitPoint.LastCommitPoint)
			mos = MethodCommitPointStatus.HasCommitPoint;
		if (seen == CommitPoint.RecurringCommitPoint)
			mos = MethodCommitPointStatus.HasRecurringCommitPoints;

		commitPointDatabase.put(method, mos);
	}

	private boolean optimizationViable(SCCUnitGraph sccUnitGraph,
			TransactionRescopingClassifier classifier) {
		List<SCC<Unit>> tails = sccUnitGraph.getSccDirectedGraph().getTails();
		if (tails.size() == 0) {
			// graph is tailless. can occur in graphs where the control never leaves a loop.
			// in such cases we don't optimize.
			return false;
		}
		for (SCC<Unit> tail : tails) {
			if (!classifier.getMustInstrument().contains(tail)) {
				// at least one tail does not need instrumentation, therefore it can be
				// optimized.
				// (we can postpone the initialization to before its next statement)
				return true;
			}
		}
		// all heads must be instrumented so not optimization is possible
		return false;
	}

	private SCCUnitGraph buildSccGraph(SootMethod method) {
		Body activeBody = method.getActiveBody();
		ExceptionalUnitGraph graph = new ExceptionalUnitGraph(activeBody);
		SCCBuilder<Unit> sccBuilder = new SCCBuilder<Unit>();
		sccBuilder.build(graph);
		return new SCCUnitGraph(method, sccBuilder.getSccDirectedGraph(),
				sccBuilder.getNodeToSccMap());
	}

}
