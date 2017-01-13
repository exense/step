package step.plugins.functions.types;

import step.functions.type.FunctionTypeConf;

public class CustomFunctionTypeConf extends FunctionTypeConf {

	String hanlderChain;

	public CustomFunctionTypeConf() {
		super();
	}

	public CustomFunctionTypeConf(String hanlderChain) {
		super();
		this.hanlderChain = hanlderChain;
	}

	public String getHanlderChain() {
		return hanlderChain;
	}

	public void setHanlderChain(String hanlderChain) {
		this.hanlderChain = hanlderChain;
	}
}
