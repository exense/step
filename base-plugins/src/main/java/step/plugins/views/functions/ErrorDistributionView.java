package step.plugins.views.functions;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;
import step.plugins.views.AbstractView;
import step.plugins.views.View;

@View
public class ErrorDistributionView extends AbstractView<ErrorDistribution> {	

	private int otherThreshhold = 500;
	
	@Override
	public void afterReportNodeSkeletonCreation(ErrorDistribution model, ReportNode node) {
	}

	@Override
	public void afterReportNodeExecution(ErrorDistribution model, ReportNode node) {
		if(node instanceof CallFunctionReportNode) {
			model.setCount(model.getCount()+1);
			if(node.getError()!=null && node.getError().getMsg()!=null) {
				model.setErrorCount(model.getErrorCount()+1);
				
				String key = node.getError().getMsg();
				Integer count = model.countByErrorMsg.get(key);
				if(count==null) {
					if(model.countByErrorMsg.size()<otherThreshhold) {
						model.countByErrorMsg.put(key, 1);
					} else {
						Integer currentOtherCount = model.countByErrorMsg.get("Other");
						if(currentOtherCount==null) {
							currentOtherCount = 0;
						}
						model.countByErrorMsg.put("Other", currentOtherCount+1);
					}
				} else {
					model.countByErrorMsg.put(key, count+1);
				}
			}
		}
	}

	@Override
	public ErrorDistribution init() {
		return new ErrorDistribution();
	}

	@Override
	public String getViewId() {
		return "errorDistribution";
	}
}
