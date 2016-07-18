package step.plugins.parametermanager;

import java.util.Map;

import step.commons.activation.Expression;
import step.commons.conf.FileRepository;
import step.commons.conf.FileRepository.FileRepositoryCallback;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;

import com.fasterxml.jackson.databind.JsonNode;

@Plugin
public class ParameterManagerPlugin extends AbstractPlugin {
	
	public static final String KEY = "ParameterManager_Instance";
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		new FileRepository<>("Parameters.js", JsonNode.class, new FileRepositoryCallback<JsonNode>() {
			@Override
			public void onLoad(JsonNode object) throws Exception {
				ParameterManager parameterManager = new ParameterManager();
				for(JsonNode o:object) {
					Expression expression = null;
					String expressionStr = o.get(2).asText();
					if(expressionStr!=null&&expressionStr.length()>0) {
						expression = new Expression(expressionStr);
					}
					Parameter p = new Parameter(expression, o.get(0).asText(), o.get(1).asText());
					if(o.has(3)) {
						p.setPriority(o.get(3).asInt());
					}
					parameterManager.addParameter(p);
				}
				context.put(KEY, parameterManager);
			}
		});
	}

	@Override
	public void executionStart(ExecutionContext context) {
		ParameterManager parameterManager = (ParameterManager) context.getGlobalContext().get(ParameterManagerPlugin.KEY);
				
		ReportNode rootNode = context.getReport();
		VariablesManager varMan = context.getVariablesManager();
		varMan.putVariable(rootNode, VariableType.RESERVED, "user", context.getExecutionParameters().getUserID());
		putVariables(context, rootNode, context.getExecutionParameters().getCustomParameters(), VariableType.RESERVED);
		
		Map<String, String> parameters = parameterManager.getAllParameters(ExecutionContextBindings.get(context));
		putVariables(context, rootNode, parameters, VariableType.IMMUTABLE);		

		super.executionStart(context);
	}
		
	private void putVariables(ExecutionContext context, ReportNode rootNode, Map<String, ? extends Object> parameters, VariableType type) {
		VariablesManager varMan = context.getVariablesManager();
		for(String key:parameters.keySet()) {
			varMan.putVariable(rootNode, type, key, parameters.get(key));
		}
	}

}
