package step.plugins.progressunit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class ProgressUnitPlugin extends AbstractPlugin {
	
	public static final String PROGRESS_UNIT_KEY = "ProgressUnitPlugin";
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		context.getServiceRegistrationCallback().registerService(ProgressUnitServices.class);
	}

	@Override
	public void executionStart(ExecutionContext context) {
		ProgressUnit unit = new ProgressUnit();
		unit.addProgressView("step.artefacts.handlers.teststep.TestStepProgressView");
		context.put(PROGRESS_UNIT_KEY, unit);
	}

	static Logger logger = LoggerFactory.getLogger(ProgressUnitPlugin.class); 
		
	private ProgressUnit getProgressUnit() {
		Object value = ExecutionContext.getCurrentContext().get(PROGRESS_UNIT_KEY);
		return value!=null?(ProgressUnit) value:null;
	}
	
	@Override
	public void afterReportNodeSkeletonCreation(ReportNode node) {
		getProgressUnit().afterReportNodeSkeletonCreation(node);
	}
	
	@Override
	public void afterReportNodeExecution(ReportNode node) {
		getProgressUnit().afterReportNodeExecution(node);
	}

}
