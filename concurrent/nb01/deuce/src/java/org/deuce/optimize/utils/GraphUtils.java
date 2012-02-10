package org.deuce.optimize.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.toolkits.graph.DirectedGraph;

public class GraphUtils {
	public static <N> List<N> getSuccsRecursively(DirectedGraph<N> graph, N node) {
		Set<N> found = new LinkedHashSet<N>();
		LinkedList<N> worklist = new LinkedList<N>();
		List<N> succsOf = graph.getSuccsOf(node);
		worklist.addAll(succsOf);

		while (!worklist.isEmpty()) {
			N current = worklist.removeFirst();
			found.add(current);
			succsOf = graph.getSuccsOf(current);
			for (N succ : succsOf) {
				if (!found.contains(succ)) {
					worklist.addLast(succ);
				}
			}
		}
		return new ArrayList<N>(found);
	}

	public static <N> List<N> getPredsRecursively(DirectedGraph<N> graph, N node) {
		Set<N> found = new LinkedHashSet<N>();
		LinkedList<N> worklist = new LinkedList<N>();
		List<N> predsOf = graph.getPredsOf(node);
		worklist.addAll(predsOf);

		while (!worklist.isEmpty()) {
			N current = worklist.removeFirst();
			found.add(current);
			predsOf = graph.getPredsOf(current);
			for (N pred : predsOf) {
				if (!found.contains(pred)) {
					worklist.addLast(pred);
				}
			}
		}
		return new ArrayList<N>(found);
	}
}
