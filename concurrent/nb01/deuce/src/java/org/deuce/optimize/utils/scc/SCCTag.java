package org.deuce.optimize.utils.scc;

import soot.tagkit.AttributeValueException;

public class SCCTag implements soot.tagkit.Tag {

	public static final String NAME = "SCC";
	private final int id;

	public SCCTag(int id) {
		this.id = id;
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return "SCCTag [id=" + id + "]";
	}

	public int getId() {
		return id;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		return null;
	}

}
