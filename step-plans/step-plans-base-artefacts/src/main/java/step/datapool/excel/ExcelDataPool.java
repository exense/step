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
package step.datapool.excel;

import ch.exense.commons.core.model.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.datapool.DataPoolConfiguration;


public class ExcelDataPool extends DataPoolConfiguration {

	DynamicValue<String> file = new DynamicValue<>();
	
	DynamicValue<String> worksheet = new DynamicValue<>();
	
	DynamicValue<Boolean> headers = new DynamicValue<>(true);

	@EntityReference(type=EntityManager.resources)
	public DynamicValue<String> getFile() {
		return file;
	}

	public void setFile(DynamicValue<String> file) {
		this.file = file;
	}

	public DynamicValue<Boolean> getHeaders() {
		return headers;
	}

	public void setHeaders(DynamicValue<Boolean> headers) {
		this.headers = headers;
	}

	public DynamicValue<String> getWorksheet() {
		return worksheet;
	}

	public void setWorksheet(DynamicValue<String> worksheet) {
		this.worksheet = worksheet;
	}
}
