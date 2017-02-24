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
		if(dynamicValue.value==null) {
			String exprType = dynamicValue.expressionType;
			Object evaluationResult = expressionHandler.evaluateGroovyExpression(dynamicValue.expression, bindings);
			dynamicValue.evalutationResult = new EvaluationResult(evaluationResult);
		}
	}

}
