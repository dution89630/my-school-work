package org.deuce.optimize.analyses.rescoping.lastfieldactivity;

import java.util.LinkedHashSet;

import org.deuce.optimize.analyses.rescoping.TransactionRescopingClassifier;
import org.deuce.optimize.utils.scc.SCC;
import org.deuce.optimize.utils.scc.SCCUnitGraph;

import soot.Unit;
import soot.toolkits.scalar.BackwardFlowAnalysis;

public class CommitPointAnalysis extends
		BackwardFlowAnalysis<SCC<Unit>, CommitStatusFlowSet> {

	private final LinkedHashSet<SCC<Unit>> mustInstrument;

	public CommitPointAnalysis(SCCUnitGraph sccUnitGraph,
			TransactionRescopingClassifier classifier) {
		super(sccUnitGraph.getSccDirectedGraph());
		this.mustInstrument = classifier.getMustInstrument();
		classifier.getCanInjectHere();
		doAnalysis();
	}

	@Override
	protected void copy(CommitStatusFlowSet source, CommitStatusFlowSet dest) {
		source.copy(dest);

	}

	@Override
	protected CommitStatusFlowSet entryInitialFlow() {
		return CommitStatusFlowSet.definitelyCommitted();
	}

	@Override
	protected void merge(CommitStatusFlowSet in1, CommitStatusFlowSet in2,
			CommitStatusFlowSet out) {
		in1.merge(in2, out);
	}

	@Override
	protected CommitStatusFlowSet newInitialFlow() {
		return CommitStatusFlowSet.definitelyCommitted();
	}

	@Override
	protected void flowThrough(CommitStatusFlowSet in, SCC<Unit> scc,
			CommitStatusFlowSet out) {
		// if !MustInstrument ==> pass the same thing, else:
		// if in == NotCommitYet ==> flow Def., and make unit Commit point.
		// if in == Poss ==> flow Def. and make unit recurring Commit point.
		// if in == Def. ==> flow Def. and make unit non-Commit point.
		// q: how to make sure all init points keep flowing until convergence?
		// a: maybe use a final pass to decide the init status of every node.
		// if that node transitions from Not to Def, it's a an init point.
		// same with Poss. to Def. - recurring init point.
		boolean mustInstrumentScc = mustInstrument.contains(scc);
		if (!mustInstrumentScc) {
			// flow value does not change
			in.copy(out);
		} else {
			// flow output must be "DefinitelyNotCommited" 
			CommitStatusFlowSet.definitelyNotCommited().copy(out);
		}
	}

}