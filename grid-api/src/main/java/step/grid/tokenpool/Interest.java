/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
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
		return selectionPattern+(must?"":" (optional)");
	}

}
