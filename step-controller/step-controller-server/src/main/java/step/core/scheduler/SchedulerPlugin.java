package step.core.scheduler;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.InputType;
import step.plugins.screentemplating.ScreenInput;
import step.plugins.screentemplating.ScreenInputAccessor;
import step.plugins.screentemplating.ScreenTemplatePlugin;

@Plugin(dependencies= {ScreenTemplatePlugin.class})
public class SchedulerPlugin extends AbstractControllerPlugin {

	private static final String SCHEDULER_TABLE = "schedulerTable";

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		
	}
	
	@Override
	public void initializeData(GlobalContext context) throws Exception {
		createScreenInputDefinitionsIfNecessary(context);
	}

	protected void createScreenInputDefinitionsIfNecessary(GlobalContext context) {
		// Plan table
		ScreenInputAccessor screenInputAccessor = context.get(ScreenInputAccessor.class);
		List<ScreenInput> screenInputsByScreenId = screenInputAccessor.getScreenInputsByScreenId(SCHEDULER_TABLE);
		Input nameInput = new Input(InputType.TEXT, "attributes.name", "Name", null, null);
		nameInput.setValueHtmlTemplate("<entity-icon entity=\"stBean\" entity-name=\"'task'\"/> <scheduler-task-link scheduler-task=\"stBean\" />");
		AtomicBoolean inputExists = new AtomicBoolean(false);
		// Force content of input 'attributes.name'
		screenInputsByScreenId.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("attributes.name")) {
				i.setInput(nameInput);
				screenInputAccessor.save(i);
				inputExists.set(true);
			}
		});
		// Create it if not existing
		if(!inputExists.get()) {
			screenInputAccessor.save(new ScreenInput(0, SCHEDULER_TABLE, nameInput));
		}
		
		if(screenInputsByScreenId.isEmpty()) {
			screenInputAccessor.save(new ScreenInput(1, SCHEDULER_TABLE, new Input(InputType.TEXT, "executionsParameters.customParameters.env", "Environment", null, null)));
		}
	}
}
