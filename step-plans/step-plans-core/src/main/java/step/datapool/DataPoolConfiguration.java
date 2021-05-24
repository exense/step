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
package step.datapool;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import ch.exense.commons.core.model.dynamicbeans.DynamicValue;

@JsonTypeInfo(use=Id.CLASS,property="_class")
public abstract class DataPoolConfiguration {

	private DynamicValue<Boolean> forWrite = new DynamicValue<Boolean>(false);

	public DynamicValue<Boolean> getForWrite() {
		return forWrite;
	}

	public void setForWrite(DynamicValue<Boolean> forWrite) {
		this.forWrite = forWrite;
	}
}
