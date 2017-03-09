package step.core.dynamicbeans;

import java.util.Map;

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
				result.setResultValue(evaluationResult);			
			} catch (Exception e) {
				result.setEvaluationException(e);
			}
			dynamicValue.evalutationResult = result;
		}
	}

}
