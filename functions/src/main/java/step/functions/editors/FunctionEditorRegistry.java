package step.functions.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionEditorRegistry {

	Map<String, List<FunctionEditor>> registry = new ConcurrentHashMap<>();
	
	public synchronized void register(String functionType, FunctionEditor functionEditor) {
		List<FunctionEditor> editors = registry.get(functionType);
		if(editors==null) {
			editors = new ArrayList<>();
			registry.put(functionType, editors);
		}
		editors.add(functionEditor);
	}
	
	public FunctionEditor getFunctionEditor(String functionType) {
		List<FunctionEditor> editors = registry.get(functionType);
		if(editors!=null) {
			return editors.get(0);
		} else {
			return null;
		}
	}
	
}
