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
package step.core.dynamicbeans;

/**
 * Properties of type ProtectedDynamicValue have access to protected bindings
 * the protected evaluation results are stored in protectedResult which is not serializable
 * @param <T>
 */
public class ProtectedDynamicValue<T> extends DynamicValue<T> {

	public ProtectedDynamicValue() {
		super();
	}

	public ProtectedDynamicValue(T value) {
		super(value);
	}

	public ProtectedDynamicValue(String expression, String expressionType) {
		super(expression, expressionType);
	}

	@Override
	protected boolean hasProtectedAccess() {
		return true;
	}

	@Override
	public DynamicValue<T> cloneValue() {
		return super._cloneValue(new ProtectedDynamicValue<>());
	}
}
