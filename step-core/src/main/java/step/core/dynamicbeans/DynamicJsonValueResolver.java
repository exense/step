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

import java.util.Map;

import jakarta.json.JsonObject;

import step.expressions.ExpressionHandler;

public class DynamicJsonValueResolver {
		
	private ExpressionHandler expressionHandler;

	public DynamicJsonValueResolver(ExpressionHandler expressionHandler) {
		super();
		this.expressionHandler = expressionHandler;
	}

	public Object evaluate(JsonObject dynamicValueAsJson, Map<String, Object> bindings) {
		boolean isDynamic = dynamicValueAsJson.getBoolean("dynamic");
		
		if(isDynamic) {
			String exprType = dynamicValueAsJson.containsKey("expressionType")?dynamicValueAsJson.getString("expressionType"):null;
			String expression = dynamicValueAsJson.getString("expression");
			try {
				return expressionHandler.evaluateGroovyExpression(expression, bindings);
			} catch (Exception e) {
				Throwable cause = e.getCause();
				String errorMsg = e.getMessage();
				if (cause != null) {
					errorMsg = errorMsg + ". Groovy error: >>> " + cause.getMessage() + " <<<";
				}
				throw new RuntimeException(errorMsg, e);
				//throw new RuntimeException("Error evaluating "+expression, e);
			}
		} else {
			return dynamicValueAsJson.get("value");
		}
	}

}
