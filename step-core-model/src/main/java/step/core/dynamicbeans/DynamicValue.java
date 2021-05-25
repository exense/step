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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DynamicValue<T> {
	
	boolean dynamic;
	
	T value;
	
	@JsonIgnore
	public EvaluationResult evalutationResult;
	
	public String expression;
	
	public String expressionType;

	public DynamicValue() {
		super();
	}

	public DynamicValue(T value) {
		super();
		this.value = value;
		this.dynamic = false;
	}

	public DynamicValue(String expression, String expressionType) {
		super();
		this.dynamic = true;
		this.expression = expression;
		this.expressionType = expressionType;
	}

	@SuppressWarnings("unchecked")
	public T get() {
		if(!isDynamic()) {
			return value;
		} else {
			if(evalutationResult!=null) {
				if(evalutationResult.evaluationException!=null) {
					Throwable cause = evalutationResult.evaluationException.getCause();
					String errorMsg = evalutationResult.evaluationException.getMessage();
					if (cause != null) {
						errorMsg = errorMsg + ". Groovy error: >>> " + cause.getMessage() + " <<<";
					}
					throw new RuntimeException(errorMsg, evalutationResult.evaluationException);
				} else {
					Object result = evalutationResult.getResultValue();
					return (T) result;					
				}
			} else {
				throw new RuntimeException("Expression hasn't been evaluated.");
			}
		}
	}

	public T get(Class<T> class_) {
		T value = get();
		return ValueConverter.convert(value, class_);
	}

	public T getOrDefault(T defaultValue) {
		T value = get();
		return value != null ? value : defaultValue;
	}

	public T getOrDefault(Class<T> class_, T defaultValue) {
		T value = get(class_);
		return value != null ? value : defaultValue;
	}

	public DynamicValue<T> cloneValue() {
		DynamicValue<T> clone = new DynamicValue<>();
		clone.dynamic = dynamic;
		clone.evalutationResult = null;
		clone.expression = expression;
		clone.expressionType = expressionType;
		clone.value = value;
		return clone;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getExpressionType() {
		return expressionType;
	}

	public void setExpressionType(String expressionType) {
		this.expressionType = expressionType;
	}
	
	public String toString() {
		return get().toString();
	}
}
