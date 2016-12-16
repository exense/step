package step.plugins.views.functions;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;

public class ReportNodeThroughputViewTest {

	@Test
	public void test() {
		ReportNodeThroughputView view = new ReportNodeThroughputView();
		ReportNodeThroughput model = view.init();
		view.setResolutions(new int[]{10,100});
		for(int j=0;j<10;j++) {
			for(int i=0;i<99;i++) {
				ReportNode node = new CallFunctionReportNode();
				node.setExecutionTime(j*100+i);
				view.afterReportNodeExecution(model, node);
			}
		}
		
		Assert.assertEquals(10,model.getIntervals().size());
		Assert.assertEquals(99,model.getIntervals().get(0l).getCount());
		Assert.assertEquals(99,model.getIntervals().get(900l).getCount());
	}
}
