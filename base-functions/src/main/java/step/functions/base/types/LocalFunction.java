package step.functions.base.types;

import step.functions.Function;

public class LocalFunction extends Function {

	@Override
	public boolean requiresLocalExecution() {
		return true;
	}
	
}
