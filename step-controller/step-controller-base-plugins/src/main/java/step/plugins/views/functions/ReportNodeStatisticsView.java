package step.plugins.views.functions;

import java.util.Map;
import java.util.Map.Entry;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.plugins.views.View;
import step.plugins.views.functions.ReportNodeStatisticsEntry.Statistics;

@View
public class ReportNodeStatisticsView extends AbstractTimeBasedView<ReportNodeStatisticsEntry> {

	@Override
	public void afterReportNodeSkeletonCreation(AbstractTimeBasedModel<ReportNodeStatisticsEntry> model, ReportNode node) {}

	@Override
	public void afterReportNodeExecution(AbstractTimeBasedModel<ReportNodeStatisticsEntry> model, ReportNode node) {
		if(node instanceof CallFunctionReportNode && node.getResolvedArtefact().isPersistNode()) {
			ReportNodeStatisticsEntry e = new ReportNodeStatisticsEntry();
			e.count = 1;
			e.sum = node.getDuration();
			Statistics stats = new Statistics(1, node.getDuration());
			Map<String, String> functionAttributes = ((CallFunctionReportNode)node).getFunctionAttributes();
			if(functionAttributes != null) {
				e.byFunctionName.put(functionAttributes.get(AbstractOrganizableObject.NAME), stats);
				addPoint(model, node.getExecutionTime(), e);
			}
		}
	}
	
	@Override
	protected void mergePoints(ReportNodeStatisticsEntry target, ReportNodeStatisticsEntry source) {
		target.count+=source.count;
		target.sum+=source.sum;
		for(Entry<String, Statistics> e:source.byFunctionName.entrySet()) {
			Statistics stats = target.byFunctionName.get(e.getKey());
			if(stats==null) {
				stats = e.getValue();
			} else {
				stats.count+=e.getValue().count;
				stats.sum+=e.getValue().sum;
			}
			target.byFunctionName.put(e.getKey(), stats);
		}
	}

	@Override
	public String getViewId() {
		return "ReportNodeStatistics";
	}
}
