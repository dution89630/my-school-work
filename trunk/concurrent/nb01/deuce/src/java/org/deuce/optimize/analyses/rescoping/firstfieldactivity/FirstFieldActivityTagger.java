package org.deuce.optimize.analyses.rescoping.firstfieldactivity;

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

public class FirstFieldActivityTagger {

	private SootMethod sootMethod;
	private FirstFieldActivityDatabase database;
	private InitPoint seen = InitPoint.Nothing;

	public void tag(SCCUnitGraph sccUnitGraph,
			FirstFieldActivityAnalysis analyzer) {
		sootMethod = sccUnitGraph.getMethod();
		database = FirstFieldActivityDatabase.getInstance();
		for (Iterator<SCC<Unit>> iterator = sccUnitGraph.getSccDirectedGraph()
				.iterator(); iterator.hasNext();) {
			SCC<Unit> scc = iterator.next();
			InitStatusFlowSet after = analyzer.getFlowAfter(scc);
			InitStatusFlowSet before = analyzer.getFlowBefore(scc);

			if (after.equals(InitStatusFlowSet.definitelyInited())) {
				if (before.equals(InitStatusFlowSet.noInitNecessary())) {
					// scc is a Initial Init Point
					addToDatabase(scc, InitPoint.InitialInitPoint);
				} else if (before.equals(InitStatusFlowSet.possiblyInited())) {
					// scc is a Recurring Init Point
					addToDatabase(scc, InitPoint.RecurringInitPoint);
				}
			}
		}
	}

	private void addToDatabase(SCC<Unit> scc, InitPoint initPoint) {
		seen = InitPoint.values()[Math.max(initPoint.ordinal(), seen.ordinal())];
		if (scc.getNodes().size() != 1) {
			// can't add an init statement in an SCC that contains more than 1 statement...
			throw new OptimizerException(
					"Impossible that we must instrument a non-singleton SCC.");
		} else {
			Set<Unit> nodes = scc.getNodes();
			for (Unit unit : nodes) {
				Stmt stmt = (Stmt) unit;
				List<UniqueCodePoint> codePoints = UniqueCodePoint.generateUniqueCodePointsFor(sootMethod, stmt);
				for (UniqueCodePoint codePoint : codePoints) {
					database.put(codePoint, initPoint);
					StmtAndMethodStorage.put(codePoint, sootMethod, stmt);				
				}				
				
				Logger.println("FFAT: " + sootMethod + ": " + unit
						+ " is a first field activity of type " + initPoint,
						sootMethod);
			}
		}
	}

	public InitPoint getSeen() {
		return seen;
	}
}
