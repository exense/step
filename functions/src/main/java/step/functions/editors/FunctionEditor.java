package step.functions.editors;

import step.functions.Function;

public abstract class FunctionEditor {

	public abstract String getEditorPath(Function function);
	
	public abstract boolean isValidForFunction(Function function);
}
