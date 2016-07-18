package step.plugins.progressunit;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.deployment.AbstractServices;
import step.core.execution.ExecutionRunnable;

@Path("/progress")
public class ProgressUnitServices extends AbstractServices {

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Progress getProgress(@PathParam("id") String executionID) {
		String reportNodeClass = "step.artefacts.reports.TestStepReportNode";
		ExecutionRunnable task = getExecutionRunnable(executionID);
		Progress p = new Progress();
		p.setReportNodeStatus(getStatusReport(executionID, reportNodeClass));
		int count = 0;
		for(Integer i:p.getReportNodeStatus().values()) {
			count+=i;
		}
		p.setCurrentProgress(count);
		p.setMaxProgress(count);
		if(task!=null && task.getContext()!=null) {
			int maxProgess = ((ProgressUnit)task.getContext().get(ProgressUnitPlugin.PROGRESS_UNIT_KEY)).getMaxProgress("step.artefacts.handlers.teststep.TestStepProgressView");
			if(maxProgess>count) {
				p.setMaxProgress(maxProgess);				
			}
		}
		return p;
	}
	
	private Map<ReportNodeStatus, Integer> getStatusReport(@PathParam("id") String executionID, @QueryParam("class") String reportNodeClass) {
		return getContext().getReportAccessor().getLeafReportNodesStatusDistribution(executionID, reportNodeClass);
	}
}
