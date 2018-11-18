package step.core.dynamicbeans;

import java.util.Map;

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
