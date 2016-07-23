package step.artefacts.handlers;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import step.artefacts.Entry;
import step.artefacts.SetVar;
import step.artefacts.reports.SetVarReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.TestArtefactResultHandler;
import step.core.miscellaneous.ValidationException;
import step.core.variables.ImmutableVariableException;
import step.core.variables.UndefinedVariableException;
import step.expressions.ExpressionHandler;

public class SetVarHandler extends ArtefactHandler<SetVar, SetVarReportNode> {

	@Override
	protected void createReportSkeleton_(SetVarReportNode node, SetVar testArtefact) {
		handleSets(node, testArtefact);
	}

	@Override
	protected void execute_(SetVarReportNode node, SetVar testArtefact) {
		handleSets(node, testArtefact);
	}
	
	private static final Pattern SET_VAR_PATTERN = Pattern.compile("[ ]*\\{(.+?)\\}[ ]*=[ ]*(.*)$");
	
	private static final Pattern SET_VAR_PATTERN_2 = Pattern.compile("[ ]*(.+?)[ ]*=[ ]*(.*)$");

	public void handleSet(String setExpression, ReportNode node) throws Exception {
		ExpressionHandler expressionHandler = new ExpressionHandler();
		if(setExpression!=null) {
			Matcher matcher = SET_VAR_PATTERN.matcher(setExpression);
			Object value;
			String variableName;
			String expression;
			if(matcher.find()) {
				variableName = matcher.group(1);
				expression = matcher.group(2);
				value = expressionHandler.evaluate(expression);
			} else {
				Matcher matcher2 = SET_VAR_PATTERN_2.matcher(setExpression);
				if(matcher2.find()) {
					variableName = matcher2.group(1);
					expression = matcher2.group(2);
					value =  expressionHandler.evaluate(expression);
				} else {
					throw new ValidationException("Falsches Format. Die set-Syntax muss lauten: set1 = \"\\{variablenName\\} = Ausdruck\", oder set1 = \"{container[index]} = Ausdruck\"");					
				}
			}
			update(variableName, value, node);
		}
	}
	
	private static Pattern mapPattern = Pattern.compile("(.+?)\\[(.+?)\\]");
	
	public void update(String name, Object value, ReportNode node) {
		Matcher matcher = mapPattern.matcher(name);
		if(matcher.find()) {
			String containerName = matcher.group(1);
			String indexName = matcher.group(2);
			
			Object object = ExecutionContext.getCurrentContext().getVariablesManager().getVariable(containerName);
			if (object != null && object instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<Object,Object> container = (Map<Object,Object>) object;
				container.put(indexName, value);
			} else {
				throw new RuntimeException("Error while evaluating the parameter " + name + 
						". A Map was expected but "+object +" was returned.");
			}
		} else {
			ExecutionContext context = ExecutionContext.getCurrentContext();
			try {
				context.getVariablesManager().updateVariable(node, name, value);
			} catch(UndefinedVariableException|ImmutableVariableException e) {
				context.getVariablesManager().putVariable(context.getReportNodeTree().getParent(node), name, value);
			}
		}
	}

	protected void handleSets(ReportNode node, SetVar testArtefact) {
		ExpressionHandler expressionHandler = new ExpressionHandler();
		
		node.setStatus(ReportNodeStatus.PASSED);
		
		for(Entry entry:testArtefact.getSets()) {
			try {
				handleSet(entry.getValue(), node);
			} catch (Exception e) {
				TestArtefactResultHandler.failWithException(node, "Error while evaluating " + entry.getKey(),e, true);
			}
		}
	}

	@Override
	public SetVarReportNode createReportNode_(ReportNode parentNode, SetVar testArtefact) {
		return new SetVarReportNode();
	}
}
