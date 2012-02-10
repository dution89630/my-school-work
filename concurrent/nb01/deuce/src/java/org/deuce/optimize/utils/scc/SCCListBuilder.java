package org.deuce.optimize.utils.scc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.toolkits.graph.DirectedGraph;

public class SCCListBuilder<T> {

	private Set<T> gray;
	private LinkedList<T> finishedOrder;
	private Set<SCC<T>> sccList;

	public Set<SCC<T>> build(DirectedGraph<T> g) {
		gray = new HashSet<T>();
		finishedOrder = new LinkedList<T>();
		sccList = new LinkedHashSet<SCC<T>>();

		Iterator<T> it = g.iterator();

		// Visit each node
		while (it.hasNext()) {
			T s = it.next();
			if (!gray.contains(s)) {
				visitNode(g, s);
			}
		}

		//Re-color all nodes white
		gray = new HashSet<T>();

		//visit nodes via tranpose edges according decreasing order of finish time of nodes
		Iterator<T> revNodeIt = finishedOrder.iterator();
		while (revNodeIt.hasNext()) {
			T s = revNodeIt.next();
			if (!gray.contains(s)) {

				SCC<T> scc = new SCC<T>();

				visitRevNode(g, s, scc);
				sccList.add(scc);
			}
		}
		return sccList;
	}

	private void visitNode(DirectedGraph<T> g, T s) {
		gray.add(s);
		Iterator<T> it = g.getSuccsOf(s).iterator();
		if (g.getSuccsOf(s).size() > 0) {
			while (it.hasNext()) {
				T succ = it.next();
				if (!gray.contains(succ)) {
					visitNode(g, succ);
				}
			}
		}
		finishedOrder.addFirst(s);
	}

	private void visitRevNode(DirectedGraph<T> g, T s, SCC<T> scc) {
		scc.add(s);
		gray.add(s);

		if (g.getPredsOf(s) != null) {
			Iterator<T> predsIt = g.getPredsOf(s).iterator();
			if (g.getPredsOf(s).size() > 0) {
				while (predsIt.hasNext()) {
					T pred = predsIt.next();
					if (!gray.contains(pred)) {
						visitRevNode(g, pred, scc);
					}
				}
			}
		}
	}
}