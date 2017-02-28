package step.functions.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import step.functions.Function;

public class FunctionEditorRegistry {

	Map<Class<? extends Function>, List<FunctionEditor<?>>> registry = new ConcurrentHashMap<>();
	
	public synchronized <T extends Function> void register(Class<T> functionClass, FunctionEditor<T> functionEditor) {
		List<FunctionEditor<?>> editors = registry.get(functionClass);
		if(editors==null) {
			editors = new ArrayList<>();
			registry.put(functionClass, editors);
		}
		editors.add(functionEditor);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Function> FunctionEditor<T> getFunctionEditor(T function) {
		List<FunctionEditor<?>> editors = registry.get(function.getClass());
		if(editors!=null) {
			return (FunctionEditor<T>) editors.get(0);
		} else {
			return null;
		}
	}
	
}
