/* Soot - a J*va Optimization Framework
 * Copyright (C) 1999 Patrice Pominville, Raja Vallee-Rai, Patrick Lam
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

package org.deuce.optimize.utils.scc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.toolkits.graph.MutableDirectedGraph;
import soot.util.Chain;
import soot.util.HashChain;

/**
 * HashMap based implementation of a MutableBlockGraph.
 */

public class DirectedGraphImpl<N> implements MutableDirectedGraph<N> {

	protected HashMap<N, LinkedHashSet<N>> nodeToPreds = new LinkedHashMap<N, LinkedHashSet<N>>();
	protected HashMap<N, LinkedHashSet<N>> nodeToSuccs = new LinkedHashMap<N, LinkedHashSet<N>>();

	protected Chain<N> heads = new HashChain<N>();
	protected Chain<N> tails = new HashChain<N>();

	public DirectedGraphImpl() {
	}

	/** Removes all nodes and edges. */
	public void clearAll() {
		nodeToPreds = new LinkedHashMap<N, LinkedHashSet<N>>();
		nodeToSuccs = new LinkedHashMap<N, LinkedHashSet<N>>();
		heads = new HashChain<N>();
		tails = new HashChain<N>();
	}

	@Override
	public DirectedGraphImpl<N> clone() {
		DirectedGraphImpl<N> g = new DirectedGraphImpl<N>();
		g.nodeToPreds = new LinkedHashMap<N, LinkedHashSet<N>>(nodeToPreds);
		g.nodeToSuccs = new LinkedHashMap<N, LinkedHashSet<N>>(nodeToSuccs);
		g.heads = new HashChain<N>(heads);
		g.tails = new HashChain<N>(tails);
		return g;
	}

	/* Returns an unbacked list of heads for this graph. */
	public List<N> getHeads() {
		ArrayList<N> l = new ArrayList<N>();
		l.addAll(heads);
		return Collections.unmodifiableList(l);
	}

	/* Returns an unbacked list of tails for this graph. */
	public List<N> getTails() {
		ArrayList<N> l = new ArrayList<N>();
		l.addAll(tails);
		return Collections.unmodifiableList(l);
	}

	public List<N> getPredsOf(N s) {
		Set<N> preds = nodeToPreds.get(s);
		if (preds != null)
			return new LinkedList<N>(preds);
		else
			throw new RuntimeException(s + "not in graph!");
	}

	/**
	 * Same as {@link #getPredsOf(N)} but returns a set.
	 * This is faster than calling {@link #getPredsOf(N)}.
	 * Also, certain operations like {@link Collection#contains(N)} execute
	 * faster on the set than on the list.
	 * The returned set is unmodifiable.
	 */
	public Set<N> getPredsOfAsSet(N s) {
		Set<N> preds = nodeToPreds.get(s);
		if (preds != null)
			return Collections.unmodifiableSet(preds);
		else
			throw new RuntimeException(s + "not in graph!");
	}

	public List<N> getSuccsOf(N s) {
		Set<N> succs = nodeToSuccs.get(s);
		if (succs != null)
			return new LinkedList<N>(succs);
		else
			throw new RuntimeException(s + "not in graph!");
	}

	/**
	 * Same as {@link #getSuccsOf(N)} but returns a set.
	 * This is faster than calling {@link #getSuccsOf(N)}.
	 * Also, certain operations like {@link Collection#contains(N)} execute
	 * faster on the set than on the list.
	 * The returned set is unmodifiable.
	 */
	public Set<N> getSuccsOfAsSet(N s) {
		Set<N> succs = nodeToSuccs.get(s);
		if (succs != null)
			return Collections.unmodifiableSet(succs);
		else
			throw new RuntimeException(s + "not in graph!");
	}

	public int size() {
		return nodeToPreds.keySet().size();
	}

	public Iterator<N> iterator() {
		return nodeToPreds.keySet().iterator();
	}

	public void addEdge(N from, N to) {
		if (from == null || to == null)
			throw new RuntimeException("edge from or to null");

		if (containsEdge(from, to))
			return;

		Set<N> succsList = nodeToSuccs.get(from);
		if (succsList == null)
			throw new RuntimeException(from + " not in graph!");

		Set<N> predsList = nodeToPreds.get(to);
		if (predsList == null)
			throw new RuntimeException(to + " not in graph!");

		if (heads.contains(to))
			heads.remove(to);

		if (tails.contains(from))
			tails.remove(from);

		succsList.add(to);
		predsList.add(from);
	}

	@Override
	public void removeEdge(N from, N to) {
		if (!containsEdge(from, to))
			return;

		Set<N> succsList = nodeToSuccs.get(from);
		if (succsList == null)
			throw new RuntimeException(from + " not in graph!");

		Set<N> predsList = nodeToPreds.get(to);
		if (predsList == null)
			throw new RuntimeException(to + " not in graph!");

		succsList.remove(to);
		predsList.remove(from);

		if (succsList.isEmpty())
			tails.add(from);

		if (predsList.isEmpty())
			heads.add(to);
	}

	@Override
	public boolean containsEdge(N from, N to) {
		Set<N> succs = nodeToSuccs.get(from);
		if (succs == null)
			return false;
		return succs.contains(to);
	}

	public boolean containsNode(N node) {
		return nodeToPreds.keySet().contains(node);
	}

	public List<N> getNodes() {
		return new ArrayList<N>(nodeToPreds.keySet());
	}

	public void addNode(N node) {
		if (containsNode(node))
			throw new RuntimeException("Node already in graph");

		nodeToSuccs.put(node, new LinkedHashSet<N>());
		nodeToPreds.put(node, new LinkedHashSet<N>());
		heads.add(node);
		tails.add(node);
	}

	@Override
	public void removeNode(N node) {
		List<N> succs = new ArrayList<N>(nodeToSuccs.get(node));
		for (Iterator<N> succsIt = succs.iterator(); succsIt.hasNext();)
			removeEdge(node, succsIt.next());
		nodeToSuccs.remove(node);

		List<N> preds = new ArrayList<N>(nodeToPreds.get(node));
		for (Iterator<N> predsIt = preds.iterator(); predsIt.hasNext();)
			removeEdge(predsIt.next(), node);
		nodeToPreds.remove(node);

		if (heads.contains(node))
			heads.remove(node);

		if (tails.contains(node))
			tails.remove(node);
	}

	@Override
	public String toString() {

		String output = "";

		for (Iterator<N> it = iterator(); it.hasNext();) {
			N node = it.next();
			output += ("Node = " + node + "\n");

			output += ("Preds:\n");
			for (Iterator<N> predsIt = getPredsOf(node).iterator(); predsIt
					.hasNext();) {
				output += ("     ");
				output += (predsIt.next() + "\n");
			}
			output += ("Succs:\n");
			for (Iterator<N> succsIt = getSuccsOf(node).iterator(); succsIt
					.hasNext();) {
				output += ("     ");
				output += (succsIt.next() + "\n");
			}
		}
		return output;
	}

}
