package org.deuce.optimize.utils.scc;

import java.util.LinkedHashSet;
import java.util.Set;

public class SCC<T> {
	private final Set<T> nodes = new LinkedHashSet<T>();
	private final int id;
	private static int counter = 0;

	public SCC() {
		this.id = counter;
		counter += 1;
	}

	public Set<T> getNodes() {
		return nodes;
	}

	public void add(T node) {
		nodes.add(node);
	}

	@Override
	public String toString() {
		return "SCC [id=" + id + ", nodes=" + nodes + "]";
	}

	public int getId() {
		return id;
	}
}
