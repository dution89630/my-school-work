package org.deuce.transaction;

import org.deuce.reflection.FieldOptimizer;

public class BasicAdvisor implements IAdvisor {

	protected int advice = 0;

	public BasicAdvisor() {
	}

	@Override
	public int visitFieldInsn(FieldOptimizer optimizer) {
		return advice;
	}

	@Override
	public String adviceGiven() {
		return "N/A";
	}

}
