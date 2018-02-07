package step.core.dynamicbeans;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DynamicValue<T> {
	
	boolean dynamic;
	
	T value;
	
	@JsonIgnore
	EvaluationResult evalutationResult;
	
	String expression;
	
	String expressionType;

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
					throw new RuntimeException("Error while evaluating expression '"+expression+"'", evalutationResult.evaluationException);
				} else {
					Object result = evalutationResult.getResultValue();
					return (T) result;					
				}
			} else {
				throw new RuntimeException("Expression hasn't been evaluated.");
			}
		}
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
