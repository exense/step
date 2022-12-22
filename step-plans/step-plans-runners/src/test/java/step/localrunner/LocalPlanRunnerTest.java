/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
import step.artefacts.CallFunction;
import step.artefacts.Echo;
import step.artefacts.FunctionGroup;
import step.artefacts.Script;
import step.artefacts.Sequence;
import step.artefacts.reports.EchoReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult; 

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
		PlanRunnerResult result = runner.run(plan);
		result.visitReportNodes(node->{
			if(node instanceof EchoReportNode) {
				Assert.assertEquals("MyProp1",((EchoReportNode) node).getEcho());
			}
		});
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testProperties2() throws IOException {
		Map<String, String> properties = new HashMap<>();
		properties.put("prop1", "MyProp1");
		
		Echo echo = new Echo();
		echo.setText(new DynamicValue<>("prop1", ""));
		Plan plan = PlanBuilder.create().startBlock(new Sequence()).add(echo).endBlock().build();
		
		runner = new LocalPlanRunner(properties, LocalRunnerTestLibrary.class);
		PlanRunnerResult result = runner.run(plan, properties);
		result.visitReportNodes(node->{
			if(node instanceof EchoReportNode) {
				Assert.assertEquals("MyProp1",((EchoReportNode) node).getEcho());
			}
		});
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
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
		PlanRunnerResult result = runner.run(plan);
		result.visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		}).printTree(tree);
		
		
		Assert.assertEquals("Sequence:PASSED:\n" + 
				" Session:PASSED:\n" + 
				"  writeSessionValue:PASSED:\n" + 
				"  assertSessionValue:PASSED:\n" + 
				" Session:PASSED:\n" + 
				"  assertSessionValue:PASSED:\n", tree.toString());
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testError() throws IOException {
		Script script = new Script();
		script.setScript("throw new Exception()");
		
		Plan plan = PlanBuilder.create().startBlock(sequence()).add(script).endBlock().build();
		
		StringWriter tree = new StringWriter();
		PlanRunnerResult result = runner.run(plan);
		result.printTree(tree);
		
		Assert.assertEquals("Sequence:TECHNICAL_ERROR:\n" + 
				" Script:TECHNICAL_ERROR:Error while running groovy expression: 'throw new Exception()'\n", tree.toString());
		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, result.getResult());
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
		PlanRunnerResult result = runner.run(plan);
		result.visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		}).printTree(tree);
		
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
		Assert.assertEquals(expectedTree, tree.toString());
	}
	
	@Test
	public void testChain() {
		Plan plan = PlanBuilder.create().startBlock(for_(1, 10))
				.add(keyword("keyword1"))
				.add(keywordWithDynamicInput("keyword2","/{\"Param1\":\"${previous.Result1}\"}/.toString()"))
				.endBlock().build();
		
		PlanRunnerResult result = runner.run(plan);
		result.visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testKeywordWithKeyValues() {
		Plan plan = PlanBuilder.create().startBlock(for_(1, 10))
				.add(keywordWithKeyValues("keyword2", "Param1","value1"))
				.endBlock().build();
		
		PlanRunnerResult result = runner.run(plan);
		result.visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
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

	@Test
	public void testKeywordWithDynamicKeyValues() {
		Plan plan = PlanBuilder.create().startBlock(for_(1, 10))
				.add(keywordWithDynamicKeyValues("keyword1", "Param1","'value1'"))
				.add(keywordWithDynamicKeyValues("keyword2", "Param1","previous.Param1"))
				.endBlock().build();
		
		PlanRunnerResult result = runner.run(plan);
		result.visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});;
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}

	@Test
	public void testKeywordOutputValues() {
		CallFunction callFunction = keywordWithDynamicKeyValues("keyword1", "string", "'value1'", "boolean", "true",
				"int", "1");
		callFunction.addChild(echo("output.string"));
		callFunction.addChild(check("output.string.toString() == \"value1\""));
		callFunction.addChild(check("output.int.toString() == \"1\""));
		callFunction.addChild(check("output.int.intValue() == 1"));
		callFunction.addChild(check("Boolean.parseBoolean(output.boolean.toString())"));
		CallFunction callFunction1 = keywordWithDynamicKeyValues("keyword1", "string2", "previous.string");
		callFunction1.addChild(check("output.string2.toString() == \"value1\""));
		Plan plan = PlanBuilder.create().startBlock(sequence())
				.add(callFunction)
				.add(callFunction1)
				.endBlock().build();

		PlanRunnerResult result = runner.run(plan);
		result.visitReportNodes(node->{
			assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});;
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
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
