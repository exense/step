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
package step.functions.accessor;

import java.util.List;

import step.core.accessors.Accessor;
import step.core.accessors.LayeredAccessor;
import step.core.collections.IndexField;
import step.functions.Function;

public class LayeredFunctionAccessor extends LayeredAccessor<Function> implements FunctionAccessor {

	public LayeredFunctionAccessor() {
		super();
	}

	public LayeredFunctionAccessor(List<FunctionAccessor> accessors) {
		super(accessors);
	}

	@Override
	public void createIndexIfNeeded(IndexField indexField) {
		Accessor<Function> accessorForPersistence = getAccessorForPersistence();
		if (accessorForPersistence instanceof FunctionAccessor) {
			((FunctionAccessor) accessorForPersistence).createIndexIfNeeded(indexField);
		}
	}
}
