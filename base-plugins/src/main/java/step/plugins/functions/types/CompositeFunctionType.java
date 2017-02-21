package step.plugins.functions.types;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import step.artefacts.Sequence;
import step.core.artefacts.AbstractArtefact;
import step.functions.Function;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;
import step.functions.type.SetupFunctionException;

@FunctionType(name="composite",label="Composite")
public class CompositeFunctionType extends AbstractFunctionType {


	@Override
	public void init() {
		super.init();
		
		context.get(FunctionEditorRegistry.class).register("composite", new FunctionEditor() {
			@Override
			public String getEditorPath(Function function) {
				return "/root/artefacteditor/"+function.getConfiguration().getString("artefactId");
			}
		});
	}
	
	@Override
	public String getHandlerChain(Function function) {
		return "class:step.core.tokenhandlers.ArtefactMessageHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(Function function) {
		Map<String, String> props = new HashMap<>();
		props.put("artefactid", function.getConfiguration().getString("artefactId"));
		return props;
	}

	@Override
	public void setupFunction(Function function) throws SetupFunctionException {
		super.setupFunction(function);
  		Sequence sequence = new Sequence();
  		context.getArtefactAccessor().save(sequence);
  		
  		JSONObject conf = buildConfiguration(sequence.getId().toString());
  		function.setConfiguration(conf);
  		
	}

	private JSONObject buildConfiguration(String artefactId) {
		JSONObject conf = new JSONObject();
  		conf.put("artefactId", artefactId);
		return conf;
	}

	@Override
	public Function copyFunction(Function function) {
		Function copy = super.copyFunction(function);

		String artefactId = function.getConfiguration().getString("artefactId");
		AbstractArtefact artefactCopy = context.getArtefactManager().copyArtefact(artefactId);
		
		copy.setConfiguration(buildConfiguration(artefactCopy.getId().toString()));
		return copy;
	}
}
