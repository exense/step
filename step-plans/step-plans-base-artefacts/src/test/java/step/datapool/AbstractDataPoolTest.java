package step.datapool;

import org.junit.Assert;
import org.junit.Test;
import step.artefacts.DataSetArtefact;
import step.artefacts.ForBlock;
import step.artefacts.Script;
import step.artefacts.handlers.AbstractArtefactHandlerTest;
import step.artefacts.reports.EchoReportNode;
import step.artefacts.reports.ForBlockReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.planbuilder.BaseArtefacts;

import java.io.IOException;

public abstract class AbstractDataPoolTest extends AbstractArtefactHandlerTest {

	protected abstract String getDataSourceType() ;

	protected abstract boolean supportDataSetUpdate();

	protected abstract DataSetArtefact getDataSetArtefact(boolean resetAtEnd) throws IOException ;

	protected abstract ForBlock getForBlock() throws IOException;

	protected abstract boolean isInMemory() ;

	@Test
	public void testDataSetRead() throws IOException {
		context = newExecutionContext();

		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(getDataSetArtefact(true))
				.startBlock(BaseArtefacts.for_(1,3))
				.add(BaseArtefacts.set("item","dataSet.next()")).add(BaseArtefacts.echo("item.Col1"))
				.endBlock().endBlock().build();
		context.getExecutionCallbacks().executionStart(context);
		execute(plan.getRoot());
		ForBlockReportNode forReportNode = (ForBlockReportNode) getChildren(getChildren(context.getReport()).get(0)).get(1);
		Assert.assertTrue(forReportNode.getStatus().equals(ReportNodeStatus.PASSED));
		Assert.assertEquals(3, forReportNode.getCount());
		Assert.assertEquals(0, forReportNode.getErrorCount());
		Assert.assertEquals("row11",((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(1)).getEcho());
		Assert.assertEquals("row21",((EchoReportNode) getChildren(getChildren(forReportNode).get(1)).get(1)).getEcho());
		Assert.assertEquals("row11",((EchoReportNode) getChildren(getChildren(forReportNode).get(2)).get(1)).getEcho());
	}

	@Test
	public void testDataSetReadError() throws IOException {
		context = newExecutionContext();

		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(getDataSetArtefact(false))
				.startBlock(BaseArtefacts.for_(1,3))
				.add(BaseArtefacts.set("item","dataSet.next()")).add(BaseArtefacts.echo("item.Col1"))
				.endBlock().endBlock().build();
		context.getExecutionCallbacks().executionStart(context);
		execute(plan.getRoot());
		ForBlockReportNode forReportNode = (ForBlockReportNode) getChildren(getChildren(context.getReport()).get(0)).get(1);
		Assert.assertTrue(forReportNode.getStatus().equals(ReportNodeStatus.TECHNICAL_ERROR));
		Assert.assertEquals(3, forReportNode.getCount());
		Assert.assertEquals(1, forReportNode.getErrorCount());
		Assert.assertEquals("row11",((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(1)).getEcho());
		Assert.assertEquals("row21",((EchoReportNode) getChildren(getChildren(forReportNode).get(1)).get(1)).getEcho());
	}

	@Test
	public void testDataSetUpdate() throws IOException {
		context = newExecutionContext();

		Script script = new Script();
		script.setScript("item.Col1 = item.Col1 + \"modified\"");
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(getDataSetArtefact(true))
				.startBlock(BaseArtefacts.for_(1,3))
				.add(BaseArtefacts.set("item","dataSet.next()")).add(BaseArtefacts.echo("item.Col1"))
				.add(script).add(BaseArtefacts.echo("item.Col1"))
				.endBlock().endBlock().build();
		context.getExecutionCallbacks().executionStart(context);
		execute(plan.getRoot());
		ForBlockReportNode forReportNode = (ForBlockReportNode) getChildren(getChildren(context.getReport()).get(0)).get(1);
		if (supportDataSetUpdate()) {
			Assert.assertTrue(forReportNode.getStatus().equals(ReportNodeStatus.PASSED));
			Assert.assertEquals(3, forReportNode.getCount());
			Assert.assertEquals(0, forReportNode.getErrorCount());
			Assert.assertEquals("row11", ((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(1)).getEcho());
			Assert.assertEquals("row11modified", ((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(3)).getEcho());
			Assert.assertEquals("row21", ((EchoReportNode) getChildren(getChildren(forReportNode).get(1)).get(1)).getEcho());
			if (!isInMemory()) {
				Assert.assertEquals("row11modified", ((EchoReportNode) getChildren(getChildren(forReportNode).get(2)).get(1)).getEcho());
				Assert.assertEquals("row11modifiedmodified", ((EchoReportNode) getChildren(getChildren(forReportNode).get(2)).get(3)).getEcho());
			}

			context.close();
			context = newExecutionContext();
			context.getExecutionCallbacks().executionStart(context);
			execute(plan.getRoot());
			forReportNode = (ForBlockReportNode) getChildren(getChildren(context.getReport()).get(0)).get(1);
			Assert.assertTrue(forReportNode.getStatus().equals(ReportNodeStatus.PASSED));
			Assert.assertEquals(3, forReportNode.getCount());
			Assert.assertEquals(0, forReportNode.getErrorCount());
			if (isInMemory()) {
				Assert.assertEquals("row11", ((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(1)).getEcho());
			} else {
				Assert.assertEquals("row11modifiedmodified", ((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(1)).getEcho());
				Assert.assertEquals("row21modified", ((EchoReportNode) getChildren(getChildren(forReportNode).get(1)).get(1)).getEcho());
			}
		} else {
			Assert.assertTrue(forReportNode.getStatus().equals(ReportNodeStatus.TECHNICAL_ERROR));
			Assert.assertEquals(3, forReportNode.getCount());
			Assert.assertEquals(3, forReportNode.getErrorCount());
			Assert.assertEquals("row11", ((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(1)).getEcho());
			Assert.assertEquals("row21", ((EchoReportNode) getChildren(getChildren(forReportNode).get(1)).get(1)).getEcho());
			Assert.assertEquals("row11", ((EchoReportNode) getChildren(getChildren(forReportNode).get(2)).get(1)).getEcho());
			Assert.assertEquals("Error while running groovy expression: 'item.Col1 = item.Col1 + \"modified\"'", getChildren(getChildren(forReportNode).get(0)).get(2).getError().getMsg());
		}
	}

	@Test
	public void testForEachRead() throws IOException {
		context = newExecutionContext();

		Plan plan = PlanBuilder.create().startBlock(getForBlock()).add(BaseArtefacts.echo("item.Col1")).endBlock().build();
		context.getExecutionCallbacks().executionStart(context);
		execute(plan.getRoot());
		ForBlockReportNode forReportNode = (ForBlockReportNode) getChildren(context.getReport()).get(0);
		Assert.assertTrue(forReportNode.getStatus().equals(ReportNodeStatus.PASSED));
		Assert.assertEquals(2, forReportNode.getCount());
		Assert.assertEquals(0, forReportNode.getErrorCount());
		Assert.assertEquals("row11",((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(0)).getEcho());
		Assert.assertEquals("row21",((EchoReportNode) getChildren(getChildren(forReportNode).get(1)).get(0)).getEcho());
	}

	@Test
	public void testForEachUpdate() throws IOException {
		context = newExecutionContext();

		Script script = new Script();
		script.setScript("item.Col1 = item.Col1 + \"modified\"");
		Plan plan = PlanBuilder.create().startBlock(getForBlock()).add(BaseArtefacts.echo("item.Col1"))
				.add(script).add(BaseArtefacts.echo("item.Col1")).endBlock().build();
		context.getExecutionCallbacks().executionStart(context);
		execute(plan.getRoot());
		ForBlockReportNode forReportNode = (ForBlockReportNode) getChildren(context.getReport()).get(0);
		Assert.assertTrue(forReportNode.getStatus().equals(ReportNodeStatus.PASSED));
		Assert.assertEquals(2, forReportNode.getCount());
		Assert.assertEquals(0, forReportNode.getErrorCount());
		Assert.assertEquals("row11",((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(0)).getEcho());
		Assert.assertEquals("row21",((EchoReportNode) getChildren(getChildren(forReportNode).get(1)).get(0)).getEcho());
		Assert.assertEquals("row11modified",((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(2)).getEcho());

		//2nd execution to validate that changes were persisted, except for in-memory data pool
		context.close();
		context = newExecutionContext();
		context.getExecutionCallbacks().executionStart(context);
		execute(plan.getRoot());
		forReportNode = (ForBlockReportNode) getChildren(context.getReport()).get(0);
		Assert.assertTrue(forReportNode.getStatus().equals(ReportNodeStatus.PASSED));
		Assert.assertEquals(2, forReportNode.getCount());
		Assert.assertEquals(0, forReportNode.getErrorCount());
		if (isInMemory()) {
			Assert.assertEquals("row11", ((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(0)).getEcho());
		} else {
			Assert.assertEquals("row11modified", ((EchoReportNode) getChildren(getChildren(forReportNode).get(0)).get(0)).getEcho());
			Assert.assertEquals("row21modified", ((EchoReportNode) getChildren(getChildren(forReportNode).get(1)).get(0)).getEcho());
		}
	}

}
