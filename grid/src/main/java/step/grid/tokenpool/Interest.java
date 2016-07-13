package step.grid.tokenpool;

import java.util.regex.Pattern;

public class Interest {
		
	Pattern selectionPattern;
	
	boolean must;

	public Interest() {
		super();
	}

	public Interest(Pattern selectionPattern, boolean must) {
		super();
		this.selectionPattern = selectionPattern;
		this.must = must;
	}
	
	public Pattern getSelectionPattern() {
		return selectionPattern;
	}

	public boolean isMust() {
		return must;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (must ? 1231 : 1237);
		result = prime
				* result
				+ ((selectionPattern == null) ? 0 : selectionPattern.toString().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Interest other = (Interest) obj;
		if (must != other.must)
			return false;
		if (selectionPattern == null) {
			if (other.selectionPattern != null)
				return false;
		} else if (!selectionPattern.toString().equals(other.selectionPattern.toString()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SelectionCriterion [pattern=" + selectionPattern + ", must=" + must + "]";
	}

}
