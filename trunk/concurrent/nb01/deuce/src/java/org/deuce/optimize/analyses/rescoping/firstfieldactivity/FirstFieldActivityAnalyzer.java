package org.deuce.optimize.analyses.rescoping.firstfieldactivity;

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

public class FirstFieldActivityAnalyzer {

	private final FirstFieldActivityDatabase firstFieldActivityDatabase;
	private final AtomicStartersDatabase atomicStartersDatabase;

	public FirstFieldActivityAnalyzer() {
		firstFieldActivityDatabase = FirstFieldActivityDatabase.getInstance();
		atomicStartersDatabase = AtomicStartersDatabase.getInstance();
	}

	public void analyzeMethod(SootMethod method) {
		firstFieldActivityDatabase.put(method,
				MethodInitPointStatus.NotOptimizable);

		if (!method.hasActiveBody()) {
			return;
		}
		if (!atomicStartersDatabase.isMethodAtomicStarter(method)) {
			return;
		}

		SCCUnitGraph sccUnitGraph = buildSccGraph(method);

		TransactionRescopingClassifier classifier = new TransactionRescopingClassifier(
				sccUnitGraph);
		classifier.classify(Direction.Backwards);

		if (!optimizationViable(sccUnitGraph, classifier)) {
			return;
		}

		internalAnalyzeMethod(method, sccUnitGraph, classifier);
	}

	private void internalAnalyzeMethod(SootMethod method,
			SCCUnitGraph sccUnitGraph, TransactionRescopingClassifier classifier) {
		Logger.println("FFAA: Analyzing method: " + method.toString(), method);

		FirstFieldActivityAnalysis analysis = new FirstFieldActivityAnalysis(
				sccUnitGraph, classifier);

		FirstFieldActivityTagger tagger = new FirstFieldActivityTagger();
		tagger.tag(sccUnitGraph, analysis);
		InitPoint seen = tagger.getSeen();
		MethodInitPointStatus mos = MethodInitPointStatus.NotOptimizable;
		if (seen == InitPoint.InitialInitPoint)
			mos = MethodInitPointStatus.HasInitPoint;
		if (seen == InitPoint.RecurringInitPoint)
			mos = MethodInitPointStatus.HasRecurringInitPoints;

		firstFieldActivityDatabase.put(method, mos);
	}

	private boolean optimizationViable(SCCUnitGraph sccUnitGraph,
			TransactionRescopingClassifier classifier) {
		List<SCC<Unit>> heads = sccUnitGraph.getSccDirectedGraph().getHeads();
		if (heads.size() == 0) {
			// graph is headless. can occur in graphs where the control immediately enters a loop.
			// in such cases we don't optimize.
			return false;
		}
		for (SCC<Unit> head : heads) {
			if (!classifier.getMustInstrument().contains(head)) {
				// at least one head does not need instrumentation, therefore it can be
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
