package org.deuce.optimize.utils.scc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.tagkit.Host;
import soot.toolkits.graph.DirectedGraph;

public class SCCBuilder<T> {
	private DirectedGraph<SCC<T>> sccDirectedGraph;

	public DirectedGraph<SCC<T>> getSccDirectedGraph() {
		return sccDirectedGraph;
	}

	public Map<T, SCC<T>> getNodeToSccMap() {
		return nodeToSccMap;
	}

	private Map<T, SCC<T>> nodeToSccMap;

	public void build(DirectedGraph<T> graph) {
		SCCListBuilder<T> listBuilder = new SCCListBuilder<T>();
		Set<SCC<T>> sccs = listBuilder.build(graph);
		nodeToSccMap = getNodeToSccMap(sccs);
		sccDirectedGraph = buildSCCGraph(graph, sccs, nodeToSccMap);
	}

	private DirectedGraph<SCC<T>> buildSCCGraph(DirectedGraph<T> graph,
			Set<SCC<T>> sccs, Map<T, SCC<T>> nodeToSccMap) {
		// get a mapping from node to its containing scc		

		DirectedGraphImpl<SCC<T>> sccGraph = new DirectedGraphImpl<SCC<T>>();
		// for each node
		for (T node : nodeToSccMap.keySet()) {
			// find its scc
			SCC<T> scc = nodeToSccMap.get(node);

			List<T> neighbors = graph.getSuccsOf(node);
			// for each neighbor
			for (T neighbor : neighbors) {
				// find its scc
				SCC<T> neighborScc = nodeToSccMap.get(neighbor);
				if (scc != neighborScc) {
					// add edge between sccs. but don't create self-loops.
					if (!sccGraph.containsNode(scc))
						sccGraph.addNode(scc);
					if (!sccGraph.containsNode(neighborScc))
						sccGraph.addNode(neighborScc);
					sccGraph.addEdge(scc, neighborScc);
				}
			}
		}

		return sccGraph;
	}

	private Map<T, SCC<T>> getNodeToSccMap(Set<SCC<T>> sccs) {
		Map<T, SCC<T>> map = new LinkedHashMap<T, SCC<T>>();
		for (SCC<T> scc : sccs) {
			for (T node : scc.getNodes()) {
				map.put(node, scc);
				if (node instanceof Host) {
					Host host = (Host)node;
					host.removeTag(SCCTag.NAME);
					((Host) node).addTag(new SCCTag(scc.getId()));
				}
			}
		}
		return map;
	}
}
