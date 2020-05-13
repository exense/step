package step.core.dynamicbeans;

import java.util.Map;

import javax.json.JsonObject;

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
