package step.engine.plugins;

import java.util.HashMap;
import java.util.Map;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionParameters;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;

@Plugin(dependencies= {})
public class BasePlugin extends AbstractExecutionEnginePlugin {

	@Override
	public void executionStart(ExecutionContext context) {
		super.executionStart(context);
		ReportNode rootNode = context.getReport();
		// Create the contextual global parameters 
		Map<String, String> globalParametersFromExecutionParameters = new HashMap<>();
		ExecutionParameters executionParameters = context.getExecutionParameters();
		if(executionParameters.getUserID() != null) {
			globalParametersFromExecutionParameters.put("user", executionParameters.getUserID());
		}
		if(executionParameters.getCustomParameters() != null) {
			globalParametersFromExecutionParameters.putAll(executionParameters.getCustomParameters());			
		}
		VariablesManager variablesManager = context.getVariablesManager();
		globalParametersFromExecutionParameters.forEach((k,v)->variablesManager.putVariable(rootNode, VariableType.IMMUTABLE, k, v));
	}
}
