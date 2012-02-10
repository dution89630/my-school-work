package org.deuce.optimize.analyses.leakyconstructor;

public class LeakyConstructorFlowSet {
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LeakyConstructorFlowSet [element=" + element + "]";
	}

	private LeakyConstructorLatticeElement element;

	private LeakyConstructorFlowSet(LeakyConstructorLatticeElement element) {
		this.element = element;
	}

	public void copy(LeakyConstructorFlowSet dest) {
		dest.element = this.element;
	}

	public void merge(LeakyConstructorFlowSet in2, LeakyConstructorFlowSet out) {
		LeakyConstructorLatticeElement mergedElement = LeakyConstructorLatticeElement
				.merge(this.element, in2.element);
		out.element = mergedElement;

	}

	public static LeakyConstructorFlowSet leaky() {
		return new LeakyConstructorFlowSet(
				LeakyConstructorLatticeElement.Leaking);
	}

	public static LeakyConstructorFlowSet unknown() {
		return new LeakyConstructorFlowSet(
				LeakyConstructorLatticeElement.Unknown);
	}

	public static LeakyConstructorFlowSet notLeaky() {
		return new LeakyConstructorFlowSet(
				LeakyConstructorLatticeElement.NotLeaking);
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
		LeakyConstructorFlowSet other = (LeakyConstructorFlowSet) obj;
		if (element == null) {
			if (other.element != null) {
				return false;
			}
		} else if (!element.equals(other.element)) {
			return false;
		}
		return true;
	}

}
