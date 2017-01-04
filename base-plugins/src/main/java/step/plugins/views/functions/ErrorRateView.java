package step.plugins.views.functions;

import java.util.Map.Entry;

import step.core.artefacts.reports.ReportNode;
import step.plugins.views.View;

@View
public class ErrorRateView extends AbstractTimeBasedView<ErrorRateEntry> {

	@Override
	public void afterReportNodeSkeletonCreation(AbstractTimeBasedModel<ErrorRateEntry> model, ReportNode node) {}

	@Override
	public void afterReportNodeExecution(AbstractTimeBasedModel<ErrorRateEntry> model, ReportNode node) {
		if(node.getError()!=null) {
			ErrorRateEntry e = new ErrorRateEntry();
			e.count = 1;
			e.countByErrorMsg.put(node.getError().getMsg(), 1);
			addPoint(model, node.getExecutionTime(), e);
		}
	}
	
	@Override
	protected void mergePoints(ErrorRateEntry target, ErrorRateEntry source) {
		target.count+=source.count;
		for(Entry<String, Integer> e:source.countByErrorMsg.entrySet()) {
			Integer count = target.countByErrorMsg.get(e.getKey());
			if(count==null) {
				count = e.getValue();
			} else {
				count = count+e.getValue();
			}
			target.countByErrorMsg.put(e.getKey(), count);
		}
	}

	@Override
	public String getViewId() {
		return "ErrorRate";
	}
}
