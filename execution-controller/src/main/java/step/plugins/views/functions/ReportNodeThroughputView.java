package step.plugins.views.functions;

import java.util.concurrent.ConcurrentHashMap;

import step.core.artefacts.reports.ReportNode;
import step.plugins.views.View;

public class ReportNodeThroughputView extends View<ReportNodeThroughput> {

	@Override
	public ReportNodeThroughput init() {
		ReportNodeThroughput model = new ReportNodeThroughput();
		model.setIntervals(new ConcurrentHashMap<>());
		return model;
	}

	@Override
	public String getViewId() {
		return "ReportNodeThroughput";
	}

	@Override
	public void afterReportNodeSkeletonCreation(ReportNodeThroughput model, ReportNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterReportNodeExecution(ReportNodeThroughput model, ReportNode node) {
		
	}

}
