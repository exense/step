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
package step.artefacts;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.artefacts.handlers.ForBlockHandler;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;

@Artefact(name = "For", handler = ForBlockHandler.class)
public class ForBlock extends AbstractForBlock {
		
	@DynamicAttribute
	String start;
	
	@DynamicAttribute
	String end;
	
	@DynamicAttribute
	String inc;

	@JsonIgnore
	public Integer getStartInt() {
		return start!=null&&start.length()>0?Integer.parseInt(start):0;
	}

	@JsonIgnore
	public Integer getEndInt() {
		return end!=null&&end.length()>0?Integer.parseInt(end):null;
	}

	@JsonIgnore
	public Integer getIncInt() {
		return inc!=null&&inc.length()>0?Integer.parseInt(inc):1;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public void setEnd(String end) {
		this.end = end;
	}

	public void setInc(String inc) {
		this.inc = inc;
	}

	public String getStart() {
		return start;
	}

	public String getEnd() {
		return end;
	}

	public String getInc() {
		return inc;
	}
}
