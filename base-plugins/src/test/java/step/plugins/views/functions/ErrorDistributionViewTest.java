package step.plugins.views.functions;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNode;

public class ErrorDistributionViewTest {

	@Test
	public void test() {
		ErrorDistributionView view = new ErrorDistributionView();

		
		ErrorDistribution model = view.init();
		for(int j=0;j<10;j++) {
			for(int i=0;i<100;i++) {
				ReportNode nodePassed = new CallFunctionReportNode();
				nodePassed.setExecutionTime(j*100+i);
				view.afterReportNodeExecution(model, nodePassed);
				
				ReportNode node = new CallFunctionReportNode();
				node.setExecutionTime(j*100+i);
				node.setError("Error "+i%2, 0, true);
				view.afterReportNodeExecution(model, node);
			}
		}
		
		Assert.assertEquals(2000,model.count);
		Assert.assertEquals(1000,model.errorCount);
		Assert.assertEquals(2,model.countByErrorMsg.size());
		Assert.assertEquals(500,(int)model.countByErrorMsg.get("Error 0"));
		Assert.assertEquals(500,(int)model.countByErrorMsg.get("Error 1"));
	}
	
	@Test
	public void testOther() {
		ErrorDistributionView view = new ErrorDistributionView();
		
		ErrorDistribution model = view.init();
		for(int j=0;j<1000;j++) {
			ReportNode node = new CallFunctionReportNode();
			node.setExecutionTime(j);
			node.setError("Error "+j, 0, true);
			view.afterReportNodeExecution(model, node);
		}
		
		Assert.assertEquals(501,model.countByErrorMsg.size());
		Assert.assertEquals(500,(int)model.countByErrorMsg.get("Other"));
	}
}
