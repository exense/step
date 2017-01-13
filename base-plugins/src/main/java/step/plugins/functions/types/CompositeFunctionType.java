package step.plugins.functions.types;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.Sequence;
import step.core.artefacts.AbstractArtefact;
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="composite",label="Composite")
public class CompositeFunctionType extends AbstractFunctionType<CompositeFunctionTypeConf> {

	@Override
	public String getHandlerChain(CompositeFunctionTypeConf functionTypeConf) {
		return "class:step.core.tokenhandlers.ArtefactMessageHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(CompositeFunctionTypeConf functionTypeConf) {
		Map<String, String> props = new HashMap<>();
		props.put("artefactid", functionTypeConf.artefactId);
		return props;
	}

	@Override
	public CompositeFunctionTypeConf newFunctionTypeConf() {
		return new CompositeFunctionTypeConf();
	}

	@Override
	public void setupFunction(Function function) {
  		Sequence sequence = new Sequence();
  		context.getArtefactAccessor().save(sequence);
  		
  		((CompositeFunctionTypeConf)function.getConfiguration()).setArtefactId(sequence.getId().toString());
  		
		super.setupFunction(function);
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

	@Override
	public String getEditorPath(Function function) {
		return "/root/artefacteditor/"+((CompositeFunctionTypeConf)function.getConfiguration()).getArtefactId();
	}

}
