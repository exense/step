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
package step.plans.nl.parser;

import java.io.IOException;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.Echo;
import step.artefacts.ForEachBlock;
import step.artefacts.TestCase;
import step.core.plans.Plan;
import step.repositories.parser.StepsParser.ParsingException;

public class PlanParserTest {
	
	@Test()
	public void testNoWrapPassed() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		parser.parse("Echo 'test'");
	}

	@Test(expected = ParsingException.class)
	public void testNoWrap() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		parser.parse("Echo 'test'\nEcho 'test'");
	}
	
	@Test(expected = ParsingException.class)
	public void testEmpty() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		parser.parse("");
	}
	
	@Test
	public void testWrap() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("Echo 'test'\nEcho 'test'");
		Assert.assertEquals(TestCase.class, plan.getRoot().getClass());
	}
	
	@Test
	public void testSingleComment() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		Plan plan = parser.parse("//first comment is the name\nEcho 'test'");
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("first comment is the name", plan.getRoot().getAttributes().get("name"));
	}
	
	@Test
	public void testComments() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		Plan plan = parser.parse("//first comment is the name\n//this is a comment\nEcho 'test'");
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("first comment is the name", plan.getRoot().getAttributes().get("name"));
		Assert.assertEquals("this is a comment", plan.getRoot().getDescription());
	}
	
	@Test
	public void testMultilineComments() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		Plan plan = parser.parse("//first comment is the name\n//this is a comment\n//on multiple lines...\nEcho 'test'");
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("first comment is the name", plan.getRoot().getAttributes().get("name"));
		Assert.assertEquals("this is a comment\non multiple lines...", plan.getRoot().getDescription());
	}
	
	@Test
	public void testDynamicStepName() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		Plan plan = parser.parse("//  |myExpression| \nEcho 'test'");
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("Echo", plan.getRoot().getAttributes().get("name"));
		Assert.assertEquals("myExpression", plan.getRoot().getDynamicName().getExpression());
	}
	
	@Test
	public void testDynamicStepNameAndComment() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		Plan plan = parser.parse("//  |myExpression| \n//This a comment\nEcho 'test'");
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("Echo", plan.getRoot().getAttributes().get("name"));
		Assert.assertEquals("myExpression", plan.getRoot().getDynamicName().getExpression());
		Assert.assertEquals("This a comment", plan.getRoot().getDescription());
	}
	
	@Test
	public void testMultiline() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		Plan plan = parser.parse(
				"-\n" +
				"Echo\n" +
				" 'test'\n" +
				"-");
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
	}
	
	@Test
	public void testMultilineComplex() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		Plan plan = parser.parse(
				"-\n" +
				"ForEach SourceType = \"excel\"\n" +
				"File = \"C:/dummy.xlsx\"\n" + 
				"Worksheet = \"myWorksheet\" Threads = 1\n" +
				"RowHandle = \"myRow\"\n" +
				"-\n"+
				"End");
		Assert.assertEquals(ForEachBlock.class, plan.getRoot().getClass());
	}
}
