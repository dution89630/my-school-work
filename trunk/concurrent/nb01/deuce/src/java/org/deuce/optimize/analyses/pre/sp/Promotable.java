package org.deuce.optimize.analyses.pre.sp;

import soot.jimple.ConcreteRef;
import soot.jimple.Constant;

public class Promotable {
	@Override
	public String toString() {
		return "Promotable [concreteRef=" + concreteRef + ", intConstant="
				+ intConstant + "]";
	}

	public Promotable(ConcreteRef concreteRef, Constant intConstant) {
		this.concreteRef = concreteRef;
		this.intConstant = intConstant;
	}

	public ConcreteRef getConcreteRef() {
		return concreteRef;
	}

	public Constant getIntConstant() {
		return intConstant;
	}

	public Promotable(ConcreteRef fieldRef) {
		this.concreteRef = fieldRef;
	}

	ConcreteRef concreteRef;
	Constant intConstant;
}
