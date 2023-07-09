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
package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.Test;

import step.artefacts.Assert;
import step.artefacts.Assert.AssertOperator;
import step.artefacts.reports.AssertReportNode;
import step.artefacts.reports.CallFunctionReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;

public class AssertHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testEquals() {
		setupPassed();

		// Test string value
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

		// Test numeric value
		setupPassed();
		a = new Assert();
		a.setActual(new DynamicValue<String>("keyInt"));
		a.setExpected(new DynamicValue<String>("777"));
		a.setOperator(AssertOperator.EQUALS);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'keyInt' expected to be equal to '777' and was '777'", child.getMessage());
		assertEquals("777", child.getExpected());
		assertEquals("777", child.getActual());
		assertEquals("keyInt = '777'", child.getDescription());

		setupPassed();
		a = new Assert();
		a.setActual(new DynamicValue<String>("keyDouble"));
		a.setExpected(new DynamicValue<String>("777.77"));
		a.setOperator(AssertOperator.EQUALS);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'keyDouble' expected to be equal to '777.77' and was '777.77'", child.getMessage());
		assertEquals("777.77", child.getExpected());
		assertEquals("777.77", child.getActual());
		assertEquals("keyDouble = '777.77'", child.getDescription());

		// Test boolean value
		setupPassed();
		a = new Assert();
		a.setActual(new DynamicValue<String>("keyBool"));
		a.setExpected(new DynamicValue<String>("true"));
		a.setOperator(AssertOperator.EQUALS);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'keyBool' expected to be equal to 'true' and was 'true'", child.getMessage());
		assertEquals("true", child.getExpected());
		assertEquals("true", child.getActual());
		assertEquals("keyBool = 'true'", child.getDescription());
	}
	
	@Test
	public void testNotEquals() {
		// Test string value
		setupPassed();
		
		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("key1"));
		a.setExpected(new DynamicValue<String>("value1"));
		a.setOperator(AssertOperator.EQUALS);
		//a.setNegate(true);
		a.setDoNegate(new DynamicValue<Boolean>(true));

		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'key1' expected not to be equal to 'value1' but was 'value1'", child.getMessage());
		assertEquals("value1", child.getExpected());
		assertEquals("value1", child.getActual());
		assertEquals("key1 != 'value1'", child.getDescription());

		// Test numeric value
		setupPassed();
		a = new Assert();
		a.setActual(new DynamicValue<String>("keyInt"));
		a.setExpected(new DynamicValue<String>("777"));
		a.setOperator(AssertOperator.EQUALS);
		a.setDoNegate(new DynamicValue<Boolean>(true));

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'keyInt' expected not to be equal to '777' but was '777'", child.getMessage());
		assertEquals("777", child.getExpected());
		assertEquals("777", child.getActual());
		assertEquals("keyInt != '777'", child.getDescription());
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
		//a.setNegate(true);
		a.setDoNegate(new DynamicValue<Boolean>(true));


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
		//a.setNegate(true);
		a.setDoNegate(new DynamicValue<Boolean>(true));


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
		//a.setNegate(true);
		a.setDoNegate(new DynamicValue<Boolean>(true));


		execute(a);
		
		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'key1' expected not to match regular expression '.*al.*' but was 'value1'", child.getMessage());
	}

	@Test
	public void testGreaterThan() {
		setupPassed();

		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("keyInt"));
		a.setExpected(new DynamicValue<String>("776"));
		a.setOperator(AssertOperator.GREATER_THAN);

		execute(a);

		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'keyInt' expected to be greater than '776' and was '777'", child.getMessage());

		setupPassed();

		a = new Assert();
		a.setActual(new DynamicValue<String>("keyInt"));
		a.setExpected(new DynamicValue<String>("778"));
		a.setOperator(AssertOperator.GREATER_THAN);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'keyInt' expected to be greater than '778' but was '777'", child.getMessage());

		// big decimal
		setupPassed();

		a = new Assert();
		a.setActual(new DynamicValue<String>("keyDouble"));
		a.setExpected(new DynamicValue<String>("777.76"));
		a.setOperator(AssertOperator.GREATER_THAN);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'keyDouble' expected to be greater than '777.76' and was '777.77'", child.getMessage());

		setupPassed();

		a = new Assert();
		a.setActual(new DynamicValue<String>("keyDouble"));
		a.setExpected(new DynamicValue<String>("777.78"));
		a.setOperator(AssertOperator.GREATER_THAN);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'keyDouble' expected to be greater than '777.78' but was '777.77'", child.getMessage());
	}

	@Test
	public void testNotGreaterThan() {
		setupPassed();

		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("keyInt"));
		a.setExpected(new DynamicValue<String>("776"));
		a.setOperator(AssertOperator.GREATER_THAN);
		a.setDoNegate(new DynamicValue<>(true));

		execute(a);

		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'keyInt' expected not to be greater than '776' but was '777'", child.getMessage());
	}

	@Test
	public void testLessThan() {
		setupPassed();

		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("keyInt"));
		a.setExpected(new DynamicValue<String>("778"));
		a.setOperator(AssertOperator.LESS_THAN);

		execute(a);

		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'keyInt' expected to be less than '778' and was '777'", child.getMessage());

		setupPassed();

		a = new Assert();
		a.setActual(new DynamicValue<String>("keyInt"));
		a.setExpected(new DynamicValue<String>("776"));
		a.setOperator(AssertOperator.LESS_THAN);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'keyInt' expected to be less than '776' but was '777'", child.getMessage());

		// big decimal
		setupPassed();

		a = new Assert();
		a.setActual(new DynamicValue<String>("keyDouble"));
		a.setExpected(new DynamicValue<String>("777.78"));
		a.setOperator(AssertOperator.LESS_THAN);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals("'keyDouble' expected to be less than '777.78' and was '777.77'", child.getMessage());

		setupPassed();

		a = new Assert();
		a.setActual(new DynamicValue<String>("keyDouble"));
		a.setExpected(new DynamicValue<String>("777.76"));
		a.setOperator(AssertOperator.LESS_THAN);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'keyDouble' expected to be less than '777.76' but was '777.77'", child.getMessage());
	}

	@Test
	public void testNotLessThan() {
		setupPassed();

		Assert a = new Assert();
		a.setActual(new DynamicValue<String>("keyInt"));
		a.setExpected(new DynamicValue<String>("778"));
		a.setOperator(AssertOperator.LESS_THAN);
		a.setDoNegate(new DynamicValue<>(true));

		execute(a);

		AssertReportNode child = (AssertReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("'keyInt' expected not to be less than '778' but was '777'", child.getMessage());
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
		// string value
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

		// int value
		setupPassed();

		a = new Assert();
		a.setActual(new DynamicValue<String>("$.key2.key2Int"));
		a.setExpected(new DynamicValue<String>("888"));
		a.setOperator(AssertOperator.EQUALS);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(ReportNodeStatus.PASSED, child.getStatus());
		assertEquals("'$.key2.key2Int' expected to be equal to '888' and was '888'", child.getMessage());

		assertEquals("888", child.getExpected());
		assertEquals("888", child.getActual());
		assertEquals("$.key2.key2Int = '888'", child.getDescription());

		// big decimal value
		setupPassed();

		a = new Assert();
		a.setActual(new DynamicValue<String>("$.key2.key2Double"));
		a.setExpected(new DynamicValue<String>("888.88"));
		a.setOperator(AssertOperator.EQUALS);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(ReportNodeStatus.PASSED, child.getStatus());
		assertEquals("'$.key2.key2Double' expected to be equal to '888.88' and was '888.88'", child.getMessage());

		assertEquals("888.88", child.getExpected());
		assertEquals("888.88", child.getActual());
		assertEquals("$.key2.key2Double = '888.88'", child.getDescription());

		// boolean value
		setupPassed();

		a = new Assert();
		a.setActual(new DynamicValue<String>("$.key2.key2Bool"));
		a.setExpected(new DynamicValue<String>("true"));
		a.setOperator(AssertOperator.EQUALS);

		execute(a);

		child = (AssertReportNode) getFirstReportNode();
		assertEquals(ReportNodeStatus.PASSED, child.getStatus());
		assertEquals("'$.key2.key2Bool' expected to be equal to 'true' and was 'true'", child.getMessage());

		assertEquals("true", child.getExpected());
		assertEquals("true", child.getActual());
		assertEquals("$.key2.key2Bool = 'true'", child.getDescription());
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
		String json = "{\"key1\":\"value1\"," +
				"\"key2\":{\"key21\":\"val21\",\"key22\":\"val22\",\"key2Int\":888,\"key2Bool\":true,\"key2Double\":888.88}, " +
				"\"keyInt\":777, " +
				"\"keyBool\":true, " +
				"\"keyDouble\":777.77}";
		JsonObject o = Json.createReader(new StringReader(json)).readObject();
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

