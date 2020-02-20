package step.plugins.views.functions;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.CallFunction;
import step.artefacts.reports.CallFunctionReportNode;

public class ReportNodeStatisticsViewTest {

	@Test
	public void test() {
		ReportNodeStatisticsView view = new ReportNodeStatisticsView();
		view.setResolutions(new int[]{10,100});
		AbstractTimeBasedModel<ReportNodeStatisticsEntry> model = view.init();
		for(int j=0;j<10;j++) {
			for(int i=0;i<99;i++) {
				CallFunctionReportNode node = new CallFunctionReportNode();
				CallFunction callFunction = new CallFunction();
				node.setArtefactInstance(callFunction);
				node.setResolvedArtefact(callFunction);
				HashMap<String,String> functionAttributes = new HashMap<>();
				functionAttributes.put("name", "Function"+i%2);
				node.setFunctionAttributes(functionAttributes);
				node.setExecutionTime(j*100+i);
				node.setDuration(i%2+1);
				view.afterReportNodeExecution(model, node);
			}
		}
		
		Assert.assertEquals(10,model.getIntervals().size());
		Assert.assertEquals(99,model.getIntervals().get(0l).getCount());
		Assert.assertEquals(99,model.getIntervals().get(900l).getCount());
		Assert.assertEquals(50,(int)model.getIntervals().get(900l).byFunctionName.get("Function0").count);
		Assert.assertEquals(49,(int)model.getIntervals().get(900l).byFunctionName.get("Function1").count);
		Assert.assertEquals(50,(int)model.getIntervals().get(900l).byFunctionName.get("Function0").sum);
		Assert.assertEquals(98,(int)model.getIntervals().get(900l).byFunctionName.get("Function1").sum);
	}
}
