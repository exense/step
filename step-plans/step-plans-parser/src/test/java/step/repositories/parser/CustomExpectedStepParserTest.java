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
package step.repositories.parser;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.artefacts.Assert.AssertOperator;
import step.artefacts.Export;
import step.artefacts.Set;
import step.core.artefacts.AbstractArtefact;
import step.repositories.parser.StepsParser.ParsingException;
import step.repositories.parser.steps.ExpectedStep;

public class CustomExpectedStepParserTest extends AbstractStepParserTest {

	@Before
	public void setUp() {
		parser = StepsParser.builder().withStepParsers(new CustomExpectedStepParser()).build();
	}
	
	@Test
	public void test() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 = \"Value1\" \n \u00A0Key2 = \"Value 2\""));
		steps.add(step("Key1 ~ \".*\""));
		
		AbstractArtefact root = parse(steps);
		Assert.assertEquals(3,getChildren(root).size());
		
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		Assert.assertEquals("\"Value1\"",check1.getExpected().getExpression());
		Assert.assertEquals(AssertOperator.EQUALS,check1.getOperator());
		Assert.assertEquals("Key1=\"Value1\"", check1.getCustomAttribute("check"));
		
		step.artefacts.Assert check2 = (step.artefacts.Assert ) getChildren(root).get(1);
		Assert.assertEquals("\"Value 2\"",check2.getExpected().getExpression());
		Assert.assertEquals("Key2=\"Value 2\"", check2.getCustomAttribute("check"));

		
		step.artefacts.Assert check3 = (step.artefacts.Assert ) getChildren(root).get(2);
		Assert.assertEquals("\".*\"",check3.getExpected().getExpression());
		Assert.assertEquals(AssertOperator.MATCHES,check3.getOperator());
	}
	
	@Test
	public void testNotEquals() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 != \"Value1\""));
		
		AbstractArtefact root = parse(steps);
		Assert.assertEquals(1,getChildren(root).size());
		
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		Assert.assertEquals("\"Value1\"",check1.getExpected().getExpression());
		Assert.assertEquals(AssertOperator.EQUALS,check1.getOperator());
		Assert.assertTrue(check1.getDoNegate().get());
	}
	
	@Test
	public void testNotBeginsWith() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 not beginsWith \"Value1\""));
		
		AbstractArtefact root = parse(steps);
		Assert.assertEquals(1,getChildren(root).size());
		
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		Assert.assertEquals("\"Value1\"",check1.getExpected().getExpression());
		Assert.assertEquals(AssertOperator.BEGINS_WITH,check1.getOperator());
		Assert.assertTrue(check1.getDoNegate().get());
	}
	
	@Test
	public void testOps() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 contains \"Value1\""));
		
		AbstractArtefact root = parse(steps);
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		checkAssert(check1, "\"Value1\"", "Key1", AssertOperator.CONTAINS, false);
	}
	
	@Test
	public void testBegins() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 beginsWith \"Value1\""));
		
		AbstractArtefact root = parse(steps);
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		checkAssert(check1, "\"Value1\"", "Key1", AssertOperator.BEGINS_WITH, false);
	}

	@Test
	public void testLessThan() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 < 777"));

		AbstractArtefact root = parse(steps);
		checkAssert((step.artefacts.Assert) getChildren(root).get(0), "777", "Key1", AssertOperator.LESS_THAN, false);

		steps.clear();
		steps.add(step("Key1 < 777.77"));

		root = parse(steps);
		checkAssert((step.artefacts.Assert) getChildren(root).get(0), "777.77", "Key1", AssertOperator.LESS_THAN, false);

		steps.clear();
		steps.add(step("Key1 < -777.77"));

		root = parse(steps);
		checkAssert((step.artefacts.Assert) getChildren(root).get(0), "-777.77", "Key1", AssertOperator.LESS_THAN, false);
	}

	@Test
	public void testGreaterThan() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 > 777"));

		AbstractArtefact root = parse(steps);
		checkAssert((step.artefacts.Assert) getChildren(root).get(0), "777", "Key1", AssertOperator.GREATER_THAN, false);

		steps.clear();
		steps.add(step("Key1 > 777.77"));

		root = parse(steps);
		checkAssert((step.artefacts.Assert) getChildren(root).get(0), "777.77", "Key1", AssertOperator.GREATER_THAN, false);

		steps.clear();
		steps.add(step("Key1 > -777.77"));

		root = parse(steps);
		checkAssert((step.artefacts.Assert) getChildren(root).get(0), "-777.77", "Key1", AssertOperator.GREATER_THAN, false);
	}

	private void checkAssert(step.artefacts.Assert check, String expectedExpression, String expectedName, AssertOperator expectedOperator, Boolean expectedDoNegate) {
		Assert.assertEquals(expectedExpression, check.getExpected().getExpression());
		Assert.assertEquals(expectedName, check.getActual().get());
		Assert.assertEquals(expectedOperator, check.getOperator());
		if(expectedDoNegate != null) {
			if (!expectedDoNegate) {
				Assert.assertFalse(check.getDoNegate().get());
			} else {
				Assert.assertTrue(check.getDoNegate().get());
			}
		}
	}

	@Test
	public void testJsonPath() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("$.key1['key2'] beginsWith \"Value1\""));
		
		AbstractArtefact root = parse(steps);
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		checkAssert(check1, "\"Value1\"", "$.key1['key2']", AssertOperator.BEGINS_WITH, false);
	}
	
	@Test
	public void testOutputAttributeWithSpaces() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("\"Key with space\" = \"Value1\""));
		steps.add(step("\"key1WithoutSpaceBetweenQuotes\" = \"Value1\""));
		
		AbstractArtefact root = parse(steps);
		Assert.assertEquals(2,getChildren(root).size());
		
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		Assert.assertEquals("\"Value1\"",check1.getExpected().getExpression());
		Assert.assertEquals("Key with space",check1.getActual().getValue());
		Assert.assertEquals(AssertOperator.EQUALS,check1.getOperator());
		
		step.artefacts.Assert check2 = (step.artefacts.Assert ) getChildren(root).get(1);
		Assert.assertEquals("\"Value1\"",check2.getExpected().getExpression());
		Assert.assertEquals("key1WithoutSpaceBetweenQuotes",check2.getActual().getValue());
		Assert.assertEquals(AssertOperator.EQUALS,check2.getOperator());
	}

	@Test
	public void testSet() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Set Key1 = Value1 \n Set Key2 = \t \"Value2\""));
		
		AbstractArtefact root = parse(steps);
		Assert.assertEquals(2,getChildren(root).size());
		
		Set check1 = (Set) getChildren(root).get(0);
		Assert.assertEquals("output.getString('Value1')",check1.getValue().getExpression());
		
		Set check2 = (Set) getChildren(root).get(1);
		Assert.assertEquals("\"Value2\"",check2.getValue().getExpression());
		
	}
	
	@Test
	public void testMultipleSet() throws Exception {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Set a=\"b\"\t c=\"d\""));
		
		List<AbstractArtefact> children = getChildren(parse(steps));
		
		Set set = (Set) children.get(0);
		Assert.assertEquals("a",set.getKey().getValue());
		Assert.assertEquals("\"b\"",set.getValue().getExpression());
		
		set = (Set) children.get(1);
		Assert.assertEquals("c",set.getKey().getValue());
		Assert.assertEquals("\"d\"",set.getValue().getExpression());
	}
	
	@Test
	public void testMixCheckSet() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 = \"Value1\" Set Key1 = Value1 Key2 != \"\""));
		
		AbstractArtefact root = parse(steps);
		Assert.assertEquals(3,getChildren(root).size());
		
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		Assert.assertEquals("\"Value1\"",check1.getExpected().getExpression());
		Assert.assertEquals("Key1",check1.getActual().get());
		Assert.assertEquals(AssertOperator.EQUALS,check1.getOperator());

		Set set1 = (Set) getChildren(root).get(1);
		Assert.assertEquals("output.getString('Value1')",set1.getValue().getExpression());
		
		step.artefacts.Assert check2 = (step.artefacts.Assert ) getChildren(root).get(2);
		Assert.assertEquals("\"\"",check2.getExpected().getExpression());
		Assert.assertEquals("Key2",check2.getActual().get());
		Assert.assertEquals(AssertOperator.EQUALS,check2.getOperator());
		Assert.assertTrue(check2.getDoNegate().get());
	}
	
	@Test
	public void testExport() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Export File = \"path/to/file\" \t Value=\"Value\" Set Key1 = Value1"));
		
		AbstractArtefact root = parse(steps);
		Assert.assertEquals(2,getChildren(root).size());
		
		Export export = (Export) getChildren(root).get(0);
		Assert.assertEquals("\"path/to/file\"",export.getFile().getExpression());
		
		Set set1 = (Set) getChildren(root).get(1);
		Assert.assertEquals("output.getString('Value1')",set1.getValue().getExpression());
	}
	
	private ExpectedStep step(String input) {
		ExpectedStep step = new ExpectedStep(input);
		return step;
	}
}
