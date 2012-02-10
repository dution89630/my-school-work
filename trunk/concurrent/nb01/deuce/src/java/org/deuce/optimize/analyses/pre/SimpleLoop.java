package org.deuce.optimize.analyses.pre;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Unit;
import soot.toolkits.graph.Block;

public class SimpleLoop {

	@Override
	public String toString() {
		return "SimpleLoop [body=" + body + ", condition=" + condition
				+ ", rest=" + rest + "]";
	}

	private final Block body;
	private final Block condition;
	private final Block rest;

	public SimpleLoop(Block body, Block condition, Block rest) {
		this.body = body;
		this.condition = condition;
		this.rest = rest;
	}

	public Block getBody() {
		return body;
	}

	public Block getCondition() {
		return condition;
	}

	public Block getRest() {
		return rest;
	}

	public List<Unit> getLoopStatements() {
		List<Unit> list = new ArrayList<Unit>();
		for (Iterator iterator = condition.iterator(); iterator.hasNext();) {
			Unit unit = (Unit) iterator.next();
			list.add(unit);
		}
		for (Iterator iterator = body.iterator(); iterator.hasNext();) {
			Unit unit = (Unit) iterator.next();
			list.add(unit);
		}
		return list;
	}
}
