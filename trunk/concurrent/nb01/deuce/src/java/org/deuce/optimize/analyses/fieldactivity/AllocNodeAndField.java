package org.deuce.optimize.analyses.fieldactivity;

import org.deuce.optimize.utils.OptimizerException;

import soot.Type;
import soot.jimple.spark.pag.AllocNode;

public class AllocNodeAndField {
	public static final String ARRAY_ELEMENTS = "$$elements$$";

	private final Object allocNodeOrType;
	private final String field;

	public AllocNodeAndField(Object allocNodeOrType, String field) {
		if (allocNodeOrType instanceof AllocNode
				|| allocNodeOrType instanceof Type) {
			this.allocNodeOrType = allocNodeOrType;
		} else {
			throw new OptimizerException(
					"allocNodeOrType parameter must be either AllocNode or Type.");
		}
		this.field = field;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AllocNodeAndField [allocNodeOrType=" + allocNodeOrType
				+ ", field=" + field + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((allocNodeOrType == null) ? 0 : allocNodeOrType.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AllocNodeAndField other = (AllocNodeAndField) obj;
		if (allocNodeOrType == null) {
			if (other.allocNodeOrType != null) {
				return false;
			}
		} else if (!allocNodeOrType.equals(other.allocNodeOrType)) {
			return false;
		}
		if (field == null) {
			if (other.field != null) {
				return false;
			}
		} else if (!field.equals(other.field)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the allocNode
	 */
	public Object getAllocNodeOrType() {
		return allocNodeOrType;
	}

	/**
	 * @return the field
	 */
	public String getField() {
		return field;
	}
}
