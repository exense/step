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
package step.functions.io;

import java.util.Map;

/**
 * This class encapsulates the arguments of a function (aka Keyword).
 * The arguments combine the payload and a list of properties.
 *
 * @param <IN>
 */
public class FunctionInput<IN> {

	protected IN payload;
	
	protected Map<String, String> properties;

	/**
	 * @return the function payload
	 */
	public IN getPayload() {
		return payload;
	}

	public void setPayload(IN payload) {
		this.payload = payload;
	}

	/**
	 * @return the list of properties in addition to the payload
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
}
