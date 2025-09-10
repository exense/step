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

import groovy.lang.GString;
import step.expressions.ExpressionHandler;
import step.expressions.ProtectedVariable;

public class DynamicValueResolver {
	
	private ExpressionHandler expressionHandler;
	
	public DynamicValueResolver(ExpressionHandler expressionHandler) {
		super();
		this.expressionHandler = expressionHandler;
	}

	public void evaluate(DynamicValue<?> dynamicValue, Map<String, Object> bindings) {
		if(dynamicValue.isDynamic()) {
			// TODO support different expression types
			String exprType = dynamicValue.expressionType;
			EvaluationResult result = new EvaluationResult();
			try {
				Object evaluationResult;
				Object protectedResult = null;

				Object o = expressionHandler.evaluateGroovyExpression(dynamicValue.expression, bindings, dynamicValue.hasProtectedAccess());
				//If access to protected bindings is granted to this dynamicValue, the protected result is added directly to the EvaluationResult
				if (dynamicValue.hasProtectedAccess() && o instanceof ProtectedVariable) {
					ProtectedVariable pb = (ProtectedVariable) o;
					protectedResult = pb.value;
					evaluationResult = pb.obfuscatedValue;
				} else {
					//Otherwise the evaluation result is return as such (ProtectedBindings remain protected)
					evaluationResult = o;
				}
				// When using placeholders in strings, groovy returns an object of type GString.
				// Curiously the class GSting doesn't extend String. For this reason we call the toString() method here
				// to avoid later casting issues when DynamicValue.get() is called
				if (evaluationResult instanceof GString) {
					evaluationResult = evaluationResult.toString();
				}
				protectedResult = (protectedResult == null) ? evaluationResult : (protectedResult instanceof GString) ? protectedResult.toString() : protectedResult;
				result.setResultValue(evaluationResult);
				result.setProtectedValue(protectedResult);
			} catch (Exception e) {
				result.setEvaluationException(e);
			}
			dynamicValue.evalutationResult = result;
		}
	}

}
