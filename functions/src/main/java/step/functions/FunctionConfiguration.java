package step.functions;

import step.commons.activation.ActivableObject;

public class FunctionConfiguration extends ActivableObject {

	String functionId;
	
	String handlerChain;

	public String getFunctionId() {
		return functionId;
	}

	public void setFunctionId(String functionId) {
		this.functionId = functionId;
	}

	public String getHandlerChain() {
		return handlerChain;
	}

	public void setHandlerChain(String handlerChain) {
		this.handlerChain = handlerChain;
	}
	
}
