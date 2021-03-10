package step.plugins.views.functions;

import org.junit.Assert;
import org.junit.Test;
import step.artefacts.handlers.ThreadGroupHandler;
import step.artefacts.reports.ThreadReportNode;

public class ThreadGroupStatisticsViewTest {

	@Test
	public void test() {
		ThreadGroupStatisticsView view = new ThreadGroupStatisticsView();
		view.setResolutions(new int[]{10,100});
		AbstractTimeBasedModel<ThreadGroupStatisticsEntry> model = view.init();
		for(int j=0;j<10;j++) {
			for(int i=0;i<100;i++) {
				ThreadReportNode node = createNode(j, i);
				view.beforeReportNodeExecution(model,node);
			}
		}
		for(int j=0;j<10;j++) {
			for(int i=0;i<100;i++) {
				ThreadReportNode node = createNode(j, i);
				view.afterReportNodeExecution(model, node);
			}
		}

		Assert.assertEquals(10,model.getIntervals().size());
		Assert.assertEquals(100,model.getIntervals().get(0l).getCount());
		Assert.assertEquals(900,model.getIntervals().get(800l).getCount());
		Assert.assertEquals(0,model.getIntervals().get(900l).getCount());
		Assert.assertEquals(100,(int)model.getIntervals().get(700l).byThreadGroupName.get("name_2").count);
		Assert.assertEquals(0,(int)model.getIntervals().get(900l).byThreadGroupName.get("name_6").count);

	}

	private ThreadReportNode createNode(int j, int i) {
		ThreadReportNode node = new ThreadReportNode();
		ThreadGroupHandler.Thread thread = new ThreadGroupHandler.Thread();
		node.setArtefactInstance(thread);
		node.setResolvedArtefact(thread);
		node.setThreadGroupName("name_" + j);
		node.setExecutionTime(j*100+i);
		node.setDuration((9-j)*100);
		return node;
	}
}
