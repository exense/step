package step.plugins.screentemplating;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import step.commons.activation.Activator;

public class ScreenTemplateManager {

	protected ScreenInputAccessor screenInputAccessor;

	public ScreenTemplateManager(ScreenInputAccessor screenInputAccessor) {
		super();
		this.screenInputAccessor = screenInputAccessor;
	}

	public List<Input> getInputsForScreen(String screenId, Map<String,Object> contextBindings) {
		List<Input> screenInputs = screenInputAccessor.getScreenInputsByScreenId(screenId).stream().map(i->i.getInput()).collect(Collectors.toList());
		
		List<Input> result = new ArrayList<>();
		List<Input> inputs =  Activator.findAllMatches(contextBindings, screenInputs);
		for(Input input:inputs) {
			List<Option> options = input.getOptions();
			List<Option> activeOptions = null;
			if(options!=null) {
				activeOptions = Activator.findAllMatches(contextBindings, options);
			}
			result.add(new Input(input.getType(), input.getId(), input.getLabel(), activeOptions));
		}
		
		return result;
	}

}
