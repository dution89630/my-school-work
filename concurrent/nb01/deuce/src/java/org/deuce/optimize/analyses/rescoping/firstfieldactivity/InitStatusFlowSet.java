package org.deuce.optimize.analyses.rescoping.firstfieldactivity;

public class InitStatusFlowSet {

	private InitStatusLatticeElement element;

	private InitStatusFlowSet(InitStatusLatticeElement element) {
		this.element = element;
	}

	public void copy(InitStatusFlowSet dest) {
		dest.element = this.element;
	}

	public void merge(InitStatusFlowSet in2, InitStatusFlowSet out) {
		InitStatusLatticeElement mergedElement = InitStatusLatticeElement
				.merge(this.element, in2.element);
		out.element = mergedElement;

	}

	public static InitStatusFlowSet noInitNecessary() {
		return new InitStatusFlowSet(InitStatusLatticeElement.NoInitNecessary);
	}

	public static InitStatusFlowSet definitelyInited() {
		return new InitStatusFlowSet(
				InitStatusLatticeElement.DefinitelyInitedAlready);
	}

	public static InitStatusFlowSet possiblyInited() {
		return new InitStatusFlowSet(
				InitStatusLatticeElement.PossiblyInitedAlready);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((element == null) ? 0 : element.hashCode());
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
		InitStatusFlowSet other = (InitStatusFlowSet) obj;
		if (element == null) {
			if (other.element != null) {
				return false;
			}
		} else if (!element.equals(other.element)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return element.toString();
	}
}
