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
package step.grid.client;

import java.util.Map;

import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;


public class TokenPretender implements Identity {

	final Map<String, String> selectionAttributes;
	
	final Map<String, Interest> interests;
	
	public TokenPretender(Map<String, String> selectionAttributes, Map<String, Interest> interests) {
		super();
		this.selectionAttributes = selectionAttributes;
		this.interests = interests;
	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getAttributes() {
		return selectionAttributes;
	}

	@Override
	public Map<String, Interest> getInterests() {
		return interests;
	}	

	@Override
	public String toString() {
		return "AdapterTokenPretender [attributes="
				+ selectionAttributes + ", selectionCriteria=" + interests + "]";
	}
}
