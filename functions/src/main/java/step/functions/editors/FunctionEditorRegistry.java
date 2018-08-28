package step.functions.editors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import step.functions.Function;

public class FunctionEditorRegistry {

	List<FunctionEditor> editors = new CopyOnWriteArrayList<>();
	
	public <T extends Function> void register(FunctionEditor functionEditor) {
		editors.add(functionEditor);
	}
	
	public FunctionEditor getFunctionEditor(Function function) {
		for(FunctionEditor editor:editors) {
			if(editor.isValidForFunction(function)) {
				return editor;
			}
		}

		return null;
	}
	
}
