package org.deuce.optimize.utils.graph;

import java.util.ArrayList;

import soot.Body;
import soot.Unit;
import soot.toolkits.exceptions.ThrowAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ExceptionalUnitGraphEx extends ExceptionalUnitGraph {
	public ExceptionalUnitGraphEx(Body body) {
		super(body);
	}

	@Override
	protected void initialize(ThrowAnalysis throwAnalysis,
			boolean omitExceptingUnitEdges) {
		super.initialize(throwAnalysis, omitExceptingUnitEdges);
		if (tails.isEmpty()) {
			// if no tails --> every unit is a tail
			tails = new ArrayList<Unit>(unitChain);
		}
		if (heads.isEmpty()) {
			// if no heads --> every unit is a head
			heads = new ArrayList<Unit>(unitChain);
		}
	}
}
