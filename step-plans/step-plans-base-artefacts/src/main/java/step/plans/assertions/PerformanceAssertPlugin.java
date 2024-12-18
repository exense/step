package step.plans.assertions;

import java.util.List;

import jakarta.json.JsonObject;

import step.artefacts.PerformanceAssert;
import step.artefacts.ThreadGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ChildrenBlock;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.Plugin;
import step.core.reports.Measure;
import step.core.variables.VariablesManager;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.functions.Function;
import step.functions.io.Output;

@Plugin
public class PerformanceAssertPlugin extends AbstractExecutionEnginePlugin {

	public static final String $PERFORMANCE_ASSERT_SESSION = "$performanceAssertSession";

	@Override
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
		//PerformanceAssert must be defined either in an after or after thread block. If any performance assert is defined we
		//create and add the performance assert session for the corresponding report node.
		AbstractArtefact resolvedArtefact = node.getResolvedArtefact();
		ChildrenBlock after = resolvedArtefact.getAfter();
		ChildrenBlock afterThread = null;
		if (resolvedArtefact instanceof ThreadGroup) {
			afterThread = ((ThreadGroup) resolvedArtefact).getAfterThread();
		}
		if ((after != null && after.getSteps().stream().anyMatch(s -> s instanceof PerformanceAssert)) ||
				(afterThread != null && afterThread.getSteps().stream().anyMatch(s -> s instanceof PerformanceAssert))) {
			PerformanceAssertSession performanceAssertSession = new PerformanceAssertSession();
			context.getVariablesManager().putVariable(node, PerformanceAssertPlugin.$PERFORMANCE_ASSERT_SESSION, performanceAssertSession);
		}
	}

	@Override
	public void afterFunctionExecution(ExecutionContext context, ReportNode node, Function function, Output<JsonObject> output) {
		VariablesManager variablesManager = context.getVariablesManager();
		List<Object> allSessions = variablesManager.getAllVariables($PERFORMANCE_ASSERT_SESSION);
		allSessions.forEach(variable->{
			PerformanceAssertSession session = (PerformanceAssertSession) variable;
			if (session != null) {
				List<Measure> measures = output.getMeasures();
				if (measures != null) {
					measures.forEach(m -> session.addMeasure(m));
				}
			}
		});
	}
}
