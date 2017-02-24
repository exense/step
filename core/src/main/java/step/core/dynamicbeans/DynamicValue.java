package step.core.dynamicbeans;

public class DynamicValue<T> {
	
	T value;
	
	EvaluationResult evalutationResult;
	
	String expression;
	
	String expressionType;

	public DynamicValue() {
		super();
	}

	public DynamicValue(T value) {
		super();
		this.value = value;
	}

	public DynamicValue(String expression, String expressionType) {
		super();
		this.expression = expression;
		this.expressionType = expressionType;
	}

	@SuppressWarnings("unchecked")
	public T get() {
		if(value!=null) {
			return value;
		} else {
			if(evalutationResult!=null) {
				Object result = evalutationResult.getResultValue();
				return (T) result;
			} else {
				throw new RuntimeException("Expression hasn't been evaluated.");
			}
		}
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
}
