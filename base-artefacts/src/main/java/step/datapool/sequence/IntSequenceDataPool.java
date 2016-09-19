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
package step.datapool.sequence;

import step.core.artefacts.DynamicAttribute;

public class IntSequenceDataPool {
	
	@DynamicAttribute
	String start;
	
	@DynamicAttribute
	String end;
	
	@DynamicAttribute
	String inc;

	public Integer getStart() {
		return start!=null&&start.length()>0?Integer.parseInt(start):0;
	}

	public Integer getEnd() {
		return end!=null&&end.length()>0?Integer.parseInt(end):null;
	}

	public Integer getInc() {
		return inc!=null&&inc.length()>0?Integer.parseInt(inc):1;
	}
}
