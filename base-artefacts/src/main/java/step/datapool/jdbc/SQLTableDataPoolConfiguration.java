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
package step.datapool.jdbc;

import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolConfiguration;


public class SQLTableDataPoolConfiguration extends DataPoolConfiguration {
	
	DynamicValue<String> connectionString = new DynamicValue<String>("");
	
	DynamicValue<String> query = new DynamicValue<String>("");

	public SQLTableDataPoolConfiguration() {
		super();
	}

	public DynamicValue<String> getConnectionString() {
		return connectionString;
	}

	public void setConnectionString(DynamicValue<String> connectionString) {
		this.connectionString = connectionString;
	}

	public DynamicValue<String> getQuery() {
		return query;
	}

	public void setQuery(DynamicValue<String> query) {
		this.query = query;
	}
}
