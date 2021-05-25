/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.datapool.sequence;

import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolConfiguration;

public class IntSequenceDataPool extends DataPoolConfiguration {
	
	DynamicValue<Integer> start = new DynamicValue<Integer>(1);
	
	DynamicValue<Integer> end = new DynamicValue<Integer>(2);
	
	DynamicValue<Integer> inc = new DynamicValue<Integer>(1);

	public IntSequenceDataPool() {
		super();
	}

	public DynamicValue<Integer> getStart() {
		return start;
	}

	public void setStart(DynamicValue<Integer> start) {
		this.start = start;
	}

	public DynamicValue<Integer> getEnd() {
		return end;
	}

	public void setEnd(DynamicValue<Integer> end) {
		this.end = end;
	}

	public DynamicValue<Integer> getInc() {
		return inc;
	}

	public void setInc(DynamicValue<Integer> inc) {
		this.inc = inc;
	}
}
