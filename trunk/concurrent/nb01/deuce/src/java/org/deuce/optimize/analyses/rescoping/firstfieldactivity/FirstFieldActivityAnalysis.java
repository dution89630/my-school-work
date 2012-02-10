package org.deuce.optimize.analyses.rescoping.firstfieldactivity;

import java.util.LinkedHashSet;

import org.deuce.optimize.analyses.rescoping.TransactionRescopingClassifier;
import org.deuce.optimize.utils.scc.SCC;
import org.deuce.optimize.utils.scc.SCCUnitGraph;

import soot.Unit;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class FirstFieldActivityAnalysis extends
		ForwardFlowAnalysis<SCC<Unit>, InitStatusFlowSet> {

	private final LinkedHashSet<SCC<Unit>> mustInstrument;

	public FirstFieldActivityAnalysis(SCCUnitGraph sccUnitGraph,
			TransactionRescopingClassifier classifier) {
		super(sccUnitGraph.getSccDirectedGraph());
		this.mustInstrument = classifier.getMustInstrument();
		doAnalysis();
	}

	@Override
	protected void copy(InitStatusFlowSet source, InitStatusFlowSet dest) {
		source.copy(dest);

	}

	@Override
	protected InitStatusFlowSet entryInitialFlow() {
		return InitStatusFlowSet.noInitNecessary();
	}

	@Override
	protected void merge(InitStatusFlowSet in1, InitStatusFlowSet in2,
			InitStatusFlowSet out) {
		in1.merge(in2, out);
	}

	@Override
	protected InitStatusFlowSet newInitialFlow() {
		return InitStatusFlowSet.noInitNecessary();
	}

	@Override
	protected void flowThrough(InitStatusFlowSet in, SCC<Unit> scc,
			InitStatusFlowSet out) {
		// if !MustInstrument ==> pass the same thing, else:
		// if in == NotInitYet ==> flow Def., and make unit Init point.
		// if in == Poss ==> flow Def. and make unit recurring Init point.
		// if in == Def. ==> flow Def. and make unit non-Init point.
		// q: how to make sure all init points keep flowing until convergence?
		// a: maybe use a final pass to decide the init status of every node.
		// if that node transitions from Not to Def, it's a an init point.
		// same with Poss. to Def. - recurring init point.
		boolean mustInstrumentScc = mustInstrument.contains(scc);
		if (!mustInstrumentScc) {
			// flow value does not change
			in.copy(out);
		} else {
			// flow output must be "DefinitelyInited"
			InitStatusFlowSet.definitelyInited().copy(out);
		}
	}

}