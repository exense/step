package step.plugins.views.functions;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;
import step.plugins.views.AbstractView;
import step.plugins.views.View;

@View
public class ErrorDistributionView extends AbstractView<ErrorDistribution> {	
	
	protected static final String DEFAULT_KEY = "Other";
	
	@Override
	public void afterReportNodeSkeletonCreation(ErrorDistribution model, ReportNode node) {
	}

	@Override
	public void afterReportNodeExecution(ErrorDistribution model, ReportNode node) {
		if(node instanceof CallFunctionReportNode && node.getResolvedArtefact().isPersistNode()) {
			model.setCount(model.getCount()+1);
			if(node.getError()!=null && node.getError().getMsg()!=null) {
				model.setErrorCount(model.getErrorCount()+1);
				model.incrementByMsg(node.getError().getMsg());
				model.incrementByCode(node.getError().getCode() == null?"default":node.getError().getCode().toString()); //temporary due to Integer type
			}
		}
	}

	@Override
	public ErrorDistribution init() {
		return new ErrorDistribution(500, DEFAULT_KEY);
	}

	@Override
	public String getViewId() {
		return "errorDistribution";
	}
}
