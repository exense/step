/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

import step.artefacts.Assert;
import step.artefacts.Assert.AssertOperator;
import step.artefacts.handlers.AbstractArtefactHandlerTest;
import step.artefacts.reports.AssertReportNode;
import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.builder.PlanBuilder;

public class AssertHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>("value1"));
		a.setOperator(AssertOperator.EQUALS);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'key1' expected to be equal to 'value1' and was 'value1'", child.getMessage());
		assertEquals("value1", child.getExpected());
		assertEquals("value1", child.getActual());
		assertEquals("key1 = 'value1'", child.getDescription());

		
	}
	
	@Test
	public void testNotEquals() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>("value1"));
		a.setOperator(AssertOperator.EQUALS);
		a.setNegate(true);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'key1' expected not to be equal to 'value1' but was 'value1'", child.getMessage());
		assertEquals("value1", child.getExpected());
		assertEquals("value1", child.getActual());
		assertEquals("key1 != 'value1'", child.getDescription());
	}
	
	@Test
	public void testContains() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>("val"));
		a.setOperator(AssertOperator.CONTAINS);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'key1' expected to contain 'val' and was 'value1'", child.getMessage());
		assertEquals("val", child.getExpected());
		assertEquals("value1", child.getActual());
		assertEquals("key1 contains 'val'", child.getDescription());
	}

	@Test
	public void testNotContains() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>("2"));
		a.setOperator(AssertOperator.CONTAINS);
		a.setNegate(true);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'key1' expected not to contain '2' and was 'value1'", child.getMessage());
		assertEquals("2", child.getExpected());
		assertEquals("value1", child.getActual());
		assertEquals("key1 not contains '2'", child.getDescription());
	}
	
	@Test
	public void testBegins() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>("val"));
		a.setOperator(AssertOperator.BEGINS_WITH);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'key1' expected to begin with 'val' and was 'value1'", child.getMessage());
	}
	
	@Test
	public void testNotBegins() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>("bal"));
		a.setOperator(AssertOperator.BEGINS_WITH);
		a.setNegate(true);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'key1' expected not to begin with 'bal' and was 'value1'", child.getMessage());
	}

	@Test
	public void testEndWith() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>("1"));
		a.setOperator(AssertOperator.ENDS_WITH);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'key1' expected to end with '1' and was 'value1'", child.getMessage());
	}
	
	@Test
	public void testMatch() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>(".*al.*"));
		a.setOperator(AssertOperator.MATCHES);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'key1' expected to match regular expression '.*al.*' and was 'value1'", child.getMessage());
	}
	
	@Test
	public void testNotMatch() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>(".*al.*"));
		a.setOperator(AssertOperator.MATCHES);
		a.setNegate(true);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'key1' expected not to match regular expression '.*al.*' but was 'value1'", child.getMessage());
	}
	
	@Test
	public void testKeyDoesntExist() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("notExistingKey"));
		a.setExpected(new DynamicValue<String>(".*al.*"));
		a.setOperator(AssertOperator.MATCHES);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("Unable to execute assertion. The keyword output doesn't contain the attribute 'notExistingKey'", child.getMessage());
		assertNull(child.getExpected());
		assertNull(child.getActual());
		assertNull(child.getDescription());
	}
	
	@Test
	public void testTechError() {
		setupTechError();
		
		Assert a = new Assert();
		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(ReportNodeStatus.NORUN, child.getStatus());
		assertNull(child.getExpected());
		assertNull(child.getActual());
		assertNull(child.getDescription());
	}
	
	@Test
	public void testJsonpath() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("$.key2.key21"));
		a.setExpected(new DynamicValue<String>("val21"));
		a.setOperator(AssertOperator.EQUALS);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(ReportNodeStatus.PASSED, child.getStatus());
		assertEquals("'$.key2.key21' expected to be equal to 'val21' and was 'val21'", child.getMessage());
		
		assertEquals("val21", child.getExpected());
		assertEquals("val21", child.getActual());
		assertEquals("$.key2.key21 = 'val21'", child.getDescription());
	}
	
	@Test
	public void testJsonpathNotFound() {
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("$.key2.notexist"));
		a.setExpected(new DynamicValue<String>("val21"));
		a.setOperator(AssertOperator.EQUALS);

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(ReportNodeStatus.FAILED, child.getStatus());
		assertEquals("No results for path: $['key2']['notexist']", child.getMessage());
	}

	private void setupPassed() {
		setupContext();
		
		CallFunctionReportNode callNode = new CallFunctionReportNode();
		JsonObject o = Json.createReader(new StringReader("{\"key1\":\"value1\",\"key2\":{\"key21\":\"val21\",\"key22\":\"val22\"}}")).readObject();
		callNode.setStatus(ReportNodeStatus.PASSED);
		callNode.setOutputObject(o);
		context.getVariablesManager().putVariable(context.getReport(),"callReport", callNode);
	}
	
	private void setupTechError() {
		setupContext();
		
		CallFunctionReportNode callNode = new CallFunctionReportNode();
		callNode.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
		context.getVariablesManager().putVariable(context.getReport(),"callReport", callNode);
	}
}

