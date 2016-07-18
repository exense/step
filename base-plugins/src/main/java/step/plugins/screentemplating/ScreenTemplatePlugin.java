package step.plugins.screentemplating;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import step.commons.activation.Activator;
import step.commons.conf.FileRepository;
import step.commons.conf.FileRepository.FileRepositoryCallback;
import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

import com.fasterxml.jackson.core.type.TypeReference;

@Plugin
public class ScreenTemplatePlugin extends AbstractPlugin {

	public static final String SCREEN_TEMPLATE_KEY = "ScreenTemplatePlugin_Instance";
	
	FileRepository<Map<String, List<Input>>> repo;
	
	Map<String, List<Input>> screenTemplates;
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		repo = new FileRepository<Map<String, List<Input>>>("ScreenTemplates.js", new TypeReference<Map<String, List<Input>>>() {}, new FileRepositoryCallback<Map<String, List<Input>>>() {
			@Override
			public void onLoad(Map<String, List<Input>> screens) throws ScriptException {
				for(String screenId:screens.keySet()) {
					List<Input> inputs = screens.get(screenId);
					Activator.compileActivationExpressions(inputs);
					for(Input input:inputs) {
						if(input.getOptions()!=null) {
							Activator.compileActivationExpressions(input.getOptions());
						}
					}	
				}
				screenTemplates = screens;
				
			}} );
		
		context.put(SCREEN_TEMPLATE_KEY, this);
		context.getServiceRegistrationCallback().registerService(ScreenTemplateService.class);
	}
	
	public List<Input> getInputsForScreen(String screenId, Map<String,Object> contextBindings) {
		assertInitialized();

		List<Input> result = new ArrayList<>();
		List<Input> inputs =  Activator.findAllMatches(contextBindings, screenTemplates.get(screenId));
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
	
	private void assertInitialized() {
		if(screenTemplates==null) {
			throw new RuntimeException("Service not initialized");
		}
	}

}
