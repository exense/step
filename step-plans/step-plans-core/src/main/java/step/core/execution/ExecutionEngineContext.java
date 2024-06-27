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
package step.core.execution;

public class ExecutionEngineContext extends AbstractExecutionEngineContext {

	public ExecutionEngineContext(OperationMode operationMode) {
		super();
		// TODO this method should be called in the constructor. All required attributes should be set explicitly
		// by the caller of this constructor in order to avoid unnecessary instantiation of the attributes as most
		// of them are replaced by the attributes of the parent context after this constructor is called
		setDefaultAttributes();
		setOperationMode(operationMode);
	}
}
