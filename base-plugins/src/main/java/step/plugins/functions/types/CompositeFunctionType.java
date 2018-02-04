package step.plugins.functions.types;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.Sequence;
import step.artefacts.handlers.CallFunctionHandler;
import step.core.artefacts.AbstractArtefact;
import step.functions.Function;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.plugins.functions.types.composite.ArtefactMessageHandler;

public class CompositeFunctionType extends AbstractFunctionType<CompositeFunction> {

	@Override
	public void init() {
		super.init();
		
		context.get(FunctionEditorRegistry.class).register(new FunctionEditor() {
			@Override
			public String getEditorPath(Function function) {
				return "/root/artefacteditor/"+((CompositeFunction)function).getArtefactId();
			}

			@Override
			public boolean isValidForFunction(Function function) {
				return function instanceof CompositeFunction;
			}
		});
	}
	
	@Override
	public String getHandlerChain(CompositeFunction function) {
		return ArtefactMessageHandler.class.getName();
	}

	@Override
	public Map<String, String> getHandlerProperties(CompositeFunction function) {
		Map<String, String> props = new HashMap<>();
		props.put(CallFunctionHandler.ARTEFACTID, function.getArtefactId());
		return props;
	}

	@Override
	public void setupFunction(CompositeFunction function) throws SetupFunctionException {
		super.setupFunction(function);
  		Sequence sequence = new Sequence();
  		context.getArtefactAccessor().save(sequence);
  		
  		function.setArtefactId(sequence.getId().toString());		
	}

	@Override
	public CompositeFunction copyFunction(CompositeFunction function) throws FunctionTypeException {
		CompositeFunction copy = super.copyFunction(function);

		String artefactId = function.getArtefactId();
		AbstractArtefact artefactCopy = context.getArtefactManager().copyArtefact(artefactId);
		
		copy.setArtefactId(artefactCopy.getId().toString());
		return copy;
	}

	@Override
	public CompositeFunction newFunction() {
		return new CompositeFunction();
	}
}
