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

import ch.exense.commons.core.model.dynamicbeans.DynamicValue;
import ch.exense.commons.core.model.dynamicbeans.EvaluationResult;
import groovy.lang.GString;
import step.expressions.ExpressionHandler;

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
				Object evaluationResult = expressionHandler.evaluateGroovyExpression(dynamicValue.expression, bindings);
				// When using placeholders in strings, groovy returns an object of type GString. 
				// Curiously the class GSting doesn't extend String. For this reason we call the toString() method here
				// to avoid later casting issues when DynamicValue.get() is called 
				if(evaluationResult instanceof GString) {
					evaluationResult = evaluationResult.toString();
				}
				result.setResultValue(evaluationResult);			
			} catch (Exception e) {
				result.setEvaluationException(e);
			}
			dynamicValue.evalutationResult = result;
		}
	}

}
