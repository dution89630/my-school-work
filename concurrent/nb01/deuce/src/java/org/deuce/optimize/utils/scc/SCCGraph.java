package org.deuce.optimize.utils.scc;

import java.util.Map;

import soot.toolkits.graph.DirectedGraph;

public class SCCGraph<T> {

	public DirectedGraph<SCC<T>> getSccDirectedGraph() {
		return sccDirectedGraph;
	}

	public Map<T, SCC<T>> getNodeToSccMap() {
		return nodeToSccMap;
	}

	@Override
	public String toString() {
		return "SCCGraph [sccDirectedGraph=" + sccDirectedGraph + "]";
	}

	protected final DirectedGraph<SCC<T>> sccDirectedGraph;
	protected final Map<T, SCC<T>> nodeToSccMap;

	public SCCGraph(DirectedGraph<SCC<T>> sccDirectedGraph,
			Map<T, SCC<T>> nodeToSccMap) {
		this.sccDirectedGraph = sccDirectedGraph;
		this.nodeToSccMap = nodeToSccMap;
	}
}