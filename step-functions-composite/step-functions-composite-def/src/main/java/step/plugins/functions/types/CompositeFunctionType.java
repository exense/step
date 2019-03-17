package step.plugins.functions.types;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import step.artefacts.Sequence;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.ArtefactManager;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.grid.bootstrap.ResourceExtractor;
import step.grid.filemanager.FileVersionId;
import step.plugins.functions.types.composite.ArtefactFunctionHandler;

public class CompositeFunctionType extends AbstractFunctionType<CompositeFunction> {
	
	private final ArtefactAccessor artefactAccessor;
	private final ArtefactManager artefactManager;
	
	protected File handlerJar;
	
	public CompositeFunctionType(ArtefactAccessor artefactAccessor, ArtefactManager artefactManager) {
		super();
		this.artefactAccessor = artefactAccessor;
		this.artefactManager = artefactManager;
	}
	
	@Override
	public void init() {
		super.init();
		handlerJar = ResourceExtractor.extractResource(getClass().getClassLoader(), "step-functions-composite-handler.jar");
	}
	
	@Override
	public String getHandlerChain(CompositeFunction function) {
		return ArtefactFunctionHandler.class.getName();
	}

	@Override
	public Map<String, String> getHandlerProperties(CompositeFunction function) {
		Map<String, String> props = new HashMap<>();
		props.put(ArtefactFunctionHandler.ARTEFACTID_KEY, function.getArtefactId());
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
	public FileVersionId getHandlerPackage(CompositeFunction function) {
		return registerFile(handlerJar);
	}

	@Override
	public CompositeFunction newFunction() {
		return new CompositeFunction();
	}
}
