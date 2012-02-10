package org.deuce.optimize.utils.scc;

public class SCCEdge<T> {
	private final SCC<T> n1;
	private final SCC<T> n2;

	public SCCEdge(SCC<T> n1, SCC<T> n2) {
		this.n1 = n1;
		this.n2 = n2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((n1 == null) ? 0 : n1.hashCode());
		result = prime * result + ((n2 == null) ? 0 : n2.hashCode());
		return result;
	}

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
		SCCEdge<SCC<T>> other = (SCCEdge<SCC<T>>) obj;
		if (n1 == null) {
			if (other.n1 != null) {
				return false;
			}
		} else if (!n1.equals(other.n1)) {
			return false;
		}
		if (n2 == null) {
			if (other.n2 != null) {
				return false;
			}
		} else if (!n2.equals(other.n2)) {
			return false;
		}
		return true;
	}

	public SCC<T> getFrom() {
		return n1;
	}

	public SCC<T> getTo() {
		return n2;
	}

	@Override
	public String toString() {
		return "SCCEdge [n1=" + n1 + ", n2=" + n2 + "]";
	}
}
