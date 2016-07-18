package step.commons.activation;


public class ActivableObject {

	protected Expression activationExpression;
	
	protected Integer priority;

	public ActivableObject() {
		super();
	}

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