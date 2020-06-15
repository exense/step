package step.localrunner;


import static org.junit.Assert.assertEquals;
import static step.planbuilder.BaseArtefacts.*;
import static step.planbuilder.FunctionArtefacts.keyword;
import static step.planbuilder.FunctionArtefacts.keywordWithDynamicInput;
import static step.planbuilder.FunctionArtefacts.keywordWithDynamicKeyValues;
import static step.planbuilder.FunctionArtefacts.keywordWithKeyValues;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.Echo;
import step.artefacts.FunctionGroup;
import step.artefacts.Script;
import step.artefacts.Sequence;
import step.artefacts.reports.EchoReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder; 

public class LocalPlanRunnerTest {
	
	protected LocalPlanRunner runner;
	
	@Before
	public void init() {
		runner = new LocalPlanRunner(LocalRunnerTestLibrary.class);
	}
	
	@Test
	public void testProperties() throws IOException {
		Map<String, String> properties = new HashMap<>();
		properties.put("prop1", "MyProp1");
		
		Echo echo = new Echo();
		echo.setText(new DynamicValue<>("prop1", ""));
		Plan plan = PlanBuilder.create().startBlock(new Sequence()).add(echo).endBlock().build();
		
		runner = new LocalPlanRunner(properties, LocalRunnerTestLibrary.class);
		runner.run(plan).visitReportNodes(node->{
			if(node instanceof EchoReportNode) {
				Assert.assertEquals("MyProp1",((EchoReportNode) node).getEcho());
			}
		});
	}
	
	@Test
	public void testSession() throws IOException {
		Plan plan = PlanBuilder.create()
				.startBlock(new Sequence())
					.startBlock(new FunctionGroup())
						.add(keyword("writeSessionValue", "{\"key\":\"myKey\", \"value\":\"myValue\"}"))
						// Assert that the key is in the Session
						.add(keyword("assertSessionValue", "{\"key\":\"myKey\", \"value\":\"myValue\"}"))
					.endBlock()
					.startBlock(new FunctionGroup())
						// Assert that the key isn't in the Session anymore as we're not in the same FunctionGroup
						.add(keyword("assertSessionValue", "{\"key\":\"myKey\", \"value\":\"\"}"))
					.endBlock()
				.endBlock()
				.build();
		
		StringWriter tree = new StringWriter();
		runner.run(plan).visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		}).printTree(tree);
		
		
		Assert.assertEquals("Sequence:PASSED:\n" + 
				" Session:PASSED:\n" + 
				"  writeSessionValue:PASSED:\n" + 
				"  assertSessionValue:PASSED:\n" + 
				" Session:PASSED:\n" + 
				"  assertSessionValue:PASSED:\n", tree.toString());
	}
	
	@Test
	public void testError() throws IOException {
		Script script = new Script();
		script.setScript("throw new Exception()");
		
		Plan plan = PlanBuilder.create().startBlock(sequence()).add(script).endBlock().build();
		
		StringWriter tree = new StringWriter();
		runner.run(plan).printTree(tree);
		
		Assert.assertEquals("Sequence:TECHNICAL_ERROR:\n" + 
				" Script:TECHNICAL_ERROR:Error while running groovy expression: 'throw new Exception()'\n", tree.toString());
	}
	
	@Test
	public void testFor() throws IOException {
		Plan plan = PlanBuilder.create().startBlock(for_(1, 10))
							.add(keyword("keyword1"))
							.add(keyword("keyword1"))
							.endBlock().build();
		
		String expectedTree = 
				"For:PASSED:\n" + 
				" Iteration 1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				" Iteration 2:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				" Iteration 3:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				" Iteration 4:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				" Iteration 5:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				" Iteration 6:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				" Iteration 7:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				" Iteration 8:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				" Iteration 9:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				" Iteration 10:PASSED:\n" + 
				"  keyword1:PASSED:\n" + 
				"  keyword1:PASSED:\n";
		
		StringWriter tree = new StringWriter();
		runner.run(plan).visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		}).printTree(tree);
		
		
		Assert.assertEquals(expectedTree, tree.toString());
	}
	
	@Test
	public void testChain() {
		Plan plan = PlanBuilder.create().startBlock(for_(1, 10))
				.add(keyword("keyword1"))
				.add(keywordWithDynamicInput("keyword2","/{\"Param1\":\"${previous.Result1}\"}/.toString()"))
				.endBlock().build();
		
		runner.run(plan).visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
	}
	
	@Test
	public void testKeywordWithKeyValues() {
		Plan plan = PlanBuilder.create().startBlock(for_(1, 10))
				.add(keywordWithKeyValues("keyword2", "Param1","value1"))
				.endBlock().build();
		
		runner.run(plan).visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
	}
	
//	private void printReportNode(ReportNode node, int level) {
//		StringBuilder builder = new StringBuilder();
//		for(int i=0;i<level;i++) {
//			builder.append(" ");
//		}
//		builder.append(node.getName()+":"+node.getStatus()+":"+(node.getError()!=null?node.getError().getMsg():""));
//		System.out.println(builder.toString());
//		
//		ReportNodeAccessor reportNodeAccessor = runner.context.getReportNodeAccessor();
//		getChildren(reportNodeAccessor, node).forEach(n->printReportNode(n, level+1));
//	}
//	
//	protected List<ReportNode> getChildren(ReportNodeAccessor reportNodeAccessor, ReportNode node) {
//		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(reportNodeAccessor.getChildren(node.getId()), Spliterator.ORDERED), false).collect(Collectors.toList());
//	}
	
	//TODO analyze why this test is failing when building in maven @Test
	public void testKeywordWithDynamicKeyValues() {
		Plan plan = PlanBuilder.create().startBlock(for_(1, 10))
				.add(keywordWithDynamicKeyValues("keyword1", "Param1","'value1'"))
				.add(keywordWithDynamicKeyValues("keyword2", "Param1","previous.Param1"))
				.endBlock().build();
		
		runner.run(plan).visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node);
		});;
	}
	
//	@Test
//	public void testForEach() {
//		Plan plan = PlanBuilder.create().startBlock(forEachRowInExcel())
//				.add(keyword("keyword1"))
//				.endBlock().build();
//		
//		runner.run(plan);
//	}
}
