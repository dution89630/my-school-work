package org.deuce.optimize.analyses.rescoping.lastfieldactivity;

public class CommitStatusFlowSet {

	private CommitStatusLatticeElement element;

	private CommitStatusFlowSet(CommitStatusLatticeElement element) {
		this.element = element;
	}

	public void copy(CommitStatusFlowSet dest) {
		dest.element = this.element;
	}

	public void merge(CommitStatusFlowSet in2, CommitStatusFlowSet out) {
		CommitStatusLatticeElement mergedElement = CommitStatusLatticeElement
				.merge(this.element, in2.element);
		out.element = mergedElement;

	}

	public static CommitStatusFlowSet definitelyCommitted() {
		return new CommitStatusFlowSet(CommitStatusLatticeElement.DefinitelyCommitted);
	}

	public static CommitStatusFlowSet definitelyNotCommited() {
		return new CommitStatusFlowSet(
				CommitStatusLatticeElement.DefinitelyNotCommitted);
	}

	public static CommitStatusFlowSet possiblyCommited() {
		return new CommitStatusFlowSet(
				CommitStatusLatticeElement.PossiblyCommitted);
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
		CommitStatusFlowSet other = (CommitStatusFlowSet) obj;
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
