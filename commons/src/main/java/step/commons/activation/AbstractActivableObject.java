package step.commons.activation;

public class AbstractActivableObject implements ActivableObject {

	Expression activationExpression;
	
	Integer priority;

	public Expression getActivationExpression() {
		return activationExpression;
	}

	public void setActivationExpression(Expression activationExpression) {
		this.activationExpression = activationExpression;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

}
