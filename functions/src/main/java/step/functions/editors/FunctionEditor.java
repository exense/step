package step.functions.editors;

import step.functions.Function;

public abstract class FunctionEditor<T extends Function> {

	public abstract String getEditorPath(T function);
}
