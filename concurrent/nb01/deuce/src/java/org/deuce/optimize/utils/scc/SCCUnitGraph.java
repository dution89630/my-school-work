package org.deuce.optimize.utils.scc;

import java.util.Map;

import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.DirectedGraph;

public class SCCUnitGraph extends SCCGraph<Unit> {

	private final SootMethod method;

	@Override
	public DirectedGraph<SCC<Unit>> getSccDirectedGraph() {
		return sccDirectedGraph;
	}

	@Override
	public Map<Unit, SCC<Unit>> getNodeToSccMap() {
		return nodeToSccMap;
	}

	public SootMethod getMethod() {
		return method;
	}

	@Override
	public String toString() {
		return "SCCUnitGraph [method=" + method + ", sccDirectedGraph="
				+ sccDirectedGraph + "]";
	}

	public SCCUnitGraph(SootMethod method,
			DirectedGraph<SCC<Unit>> sccDirectedGraph,
			Map<Unit, SCC<Unit>> nodeToSccMap) {
		super(sccDirectedGraph, nodeToSccMap);
		this.method = method;
	}
}