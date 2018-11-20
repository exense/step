package step.plugins.functions.types;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.Sequence;
import step.artefacts.handlers.CallFunctionHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.ArtefactManager;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.plugins.functions.types.composite.ArtefactFunctionHandler;

public class CompositeFunctionType extends AbstractFunctionType<CompositeFunction> {

	private final ArtefactAccessor artefactAccessor;
	private final ArtefactManager artefactManager;

	public CompositeFunctionType(ArtefactAccessor artefactAccessor, ArtefactManager artefactManager) {
		super();
		this.artefactAccessor = artefactAccessor;
		this.artefactManager = artefactManager;
	}

	@Override
	public void init() {
		super.init();
	}
	
	@Override
	public String getHandlerChain(CompositeFunction function) {
		return ArtefactFunctionHandler.class.getName();
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
  		artefactAccessor.save(sequence);
  		
  		function.setArtefactId(sequence.getId().toString());		
	}

	@Override
	public CompositeFunction copyFunction(CompositeFunction function) throws FunctionTypeException {
		CompositeFunction copy = super.copyFunction(function);

		String artefactId = function.getArtefactId();
		AbstractArtefact artefactCopy = artefactManager.copyArtefact(artefactId);
		
		copy.setArtefactId(artefactCopy.getId().toString());
		return copy;
	}

	@Override
	public CompositeFunction newFunction() {
		return new CompositeFunction();
	}
}
