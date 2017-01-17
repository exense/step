package step.plugins.functions.types;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.Sequence;
import step.core.artefacts.AbstractArtefact;
import step.functions.Function;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="composite",label="Composite")
public class CompositeFunctionType extends AbstractFunctionType<CompositeFunctionTypeConf> {


	@Override
	public void init() {
		super.init();
		
		context.get(FunctionEditorRegistry.class).register("composite", new FunctionEditor() {
			@Override
			public String getEditorPath(Function function) {
				return "/root/artefacteditor/"+((CompositeFunctionTypeConf)function.getConfiguration()).getArtefactId();
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
		props.put("artefactid", getFunctionConf(function).artefactId);
		return props;
	}

	@Override
	public CompositeFunctionTypeConf newFunctionTypeConf() {
		CompositeFunctionTypeConf conf = new CompositeFunctionTypeConf();
		conf.setCallTimeout(180000);
		return conf;
	}

	@Override
	public void setupFunction(Function function) {
		super.setupFunction(function);
  		Sequence sequence = new Sequence();
  		context.getArtefactAccessor().save(sequence);
  		
  		((CompositeFunctionTypeConf)function.getConfiguration()).setArtefactId(sequence.getId().toString());
  		
	}

	@Override
	public Function copyFunction(Function function) {
		Function copy = super.copyFunction(function);
		CompositeFunctionTypeConf conf = ((CompositeFunctionTypeConf)function.getConfiguration());
		
		String artefactId = conf.getArtefactId();
		AbstractArtefact artefactCopy = context.getArtefactManager().copyArtefact(artefactId);
		conf.setArtefactId(artefactCopy.getId().toString());
		return copy;
	}
}
