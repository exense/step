package step.artefacts;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.engine.plugins.base.ResourceManagerPlugin;

public class AbstractArtefactTest {

	public AbstractArtefactTest() {
		super();
	}

	protected ExecutionContext newExecutionContext() {
		return ExecutionEngine.builder().withPlugin(new ResourceManagerPlugin()).build().newExecutionContext();
	}

}