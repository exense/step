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

import org.junit.Assert;
import step.artefacts.*;
import step.artefacts.ThreadGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.plans.nl.RootArtefactType;
import step.repositories.parser.StepsParser;
import step.repositories.parser.StepsParser.ParsingException;

public class PlanParserTest {

	@Test()
	public void testNoWrapPassed() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.parse("Echo 'test'", null);
	}

	@Test(expected = ParsingException.class)
	public void testNoWrap() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.parse("Echo 'test'\nEcho 'test'", null);
	}

	@Test()
	public void testWrappedInSequence() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("Echo 'test'\nEcho 'test'", RootArtefactType.Sequence);
		Assert.assertEquals(Sequence.class, plan.getRoot().getClass());
	}

	@Test()
	public void testWrappedInTestCase() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("Echo 'test'\nEcho 'test'", RootArtefactType.TestCase);
		Assert.assertEquals(TestCase.class, plan.getRoot().getClass());
	}

	@Test()
	public void testWrappedInTestSet() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("Echo 'test'\nEcho 'test'", RootArtefactType.TestSet);
		Assert.assertEquals(TestSet.class, plan.getRoot().getClass());
	}

	@Test()
	public void testWrappedInTestScenario() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("Echo 'test'\nEcho 'test'", RootArtefactType.TestScenario);
		Assert.assertEquals(TestScenario.class, plan.getRoot().getClass());
	}

	@Test()
	public void testWrappedInThreadGroup() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("Echo 'test'\nEcho 'test'", RootArtefactType.ThreadGroup);
		Assert.assertEquals(ThreadGroup.class, plan.getRoot().getClass());
	}

	@Test(expected = ParsingException.class)
	public void testEmpty() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.parse("", null);
	}

	@Test()
	public void testEmptySequence() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("", RootArtefactType.Sequence);
		Assert.assertEquals(Sequence.class, plan.getRoot().getClass());
	}

	@Test
	public void testWrap() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("Echo 'test'\nEcho 'test'", RootArtefactType.TestCase);
		Assert.assertEquals(TestCase.class, plan.getRoot().getClass());
	}

	@Test
	public void testSingleComment() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("//first comment is the name\nEcho 'test'", null);
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("first comment is the name", plan.getRoot().getAttributes().get("name"));
	}

	@Test
	public void testComments() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("//first comment is the name\n//this is a comment\nEcho 'test'", null);
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("first comment is the name", plan.getRoot().getAttributes().get("name"));
		Assert.assertEquals("this is a comment", plan.getRoot().getDescription());
	}

	@Test
	public void testMultilineComments() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("//first comment is the name\n//this is a comment\n//on multiple lines...\nEcho 'test'", null);
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("first comment is the name", plan.getRoot().getAttributes().get("name"));
		Assert.assertEquals("this is a comment\non multiple lines...", plan.getRoot().getDescription());
	}

	@Test
	public void testDynamicStepName() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("//  |myExpression| \nEcho 'test'", null);
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("Echo", plan.getRoot().getAttributes().get("name"));
		Assert.assertEquals("myExpression", plan.getRoot().getDynamicName().getExpression());
	}

	@Test
	public void testDynamicStepNameAndComment() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse("//  |myExpression| \n//This a comment\nEcho 'test'", null);
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("Echo", plan.getRoot().getAttributes().get("name"));
		Assert.assertEquals("myExpression", plan.getRoot().getDynamicName().getExpression());
		Assert.assertEquals("This a comment", plan.getRoot().getDescription());
	}

	@Test
	public void testMultiline() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse(
				"-\n" +
				"Echo\n" +
				" 'test'\n" +
				"-", null);
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
	}

	@Test
	public void testMultilineComplex() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		Plan plan = parser.parse(
				"-\n" +
				"ForEach SourceType = \"excel\"\n" +
				"File = \"C:/dummy.xlsx\"\n" +
				"Worksheet = \"myWorksheet\" Threads = 1\n" +
				"RowHandle = \"myRow\"\n" +
				"-\n"+
				"End", null);
		Assert.assertEquals(ForEachBlock.class, plan.getRoot().getClass());
	}

	@Test
	public void testComplexPlans() throws StepsParser.ParsingException {

		PlanParser parser = new PlanParser();

		Plan parsedPlan = parser.parse(getClass().getResourceAsStream("complexPlan.plan"), RootArtefactType.Sequence);
		AbstractArtefact root = parsedPlan.getRoot();
		Assert.assertEquals(1,root.getChildren().size());

		ForBlock forBlock = (ForBlock) root.getChildren().get(0);
		Assert.assertEquals("For loop", forBlock.getAttributes().get("name"));

		Sequence wrappingSequence = (Sequence) forBlock.getChildren().get(0);
		Assert.assertEquals("Sequence", wrappingSequence.getAttributes().get("name"));

		Assert.assertEquals(4, wrappingSequence.getChildren().size());

		CallFunction callFunction = (CallFunction) wrappingSequence.getChildren().get(0);
		Assert.assertEquals("Call a keyword", callFunction.getAttributes().get("name"));
		Assert.assertEquals(1, callFunction.getChildren().size());

		CallFunction callFunction2 = (CallFunction) wrappingSequence.getChildren().get(1);
		Assert.assertEquals("MyOtherKeyword", callFunction2.getAttributes().get("name"));
		Assert.assertEquals(1, callFunction.getChildren().size());

		Assert.assertEquals("some echo in iteration", wrappingSequence.getChildren().get(2).getAttributes().get("name"));
		Assert.assertEquals("some other echo in iteration", wrappingSequence.getChildren().get(3).getAttributes().get("name"));

		Echo echo = (Echo) wrappingSequence.getAfter().getSteps().get(0);
		Assert.assertEquals("some echo in afterSequence", echo.getAttributes().get("name"));

		Echo echoBeforeSequence = (Echo) wrappingSequence.getBefore().getSteps().get(0);
		Assert.assertEquals("some echo in before sequence", echoBeforeSequence.getAttributes().get("name"));

		Echo echoAfter = (Echo) forBlock.getAfter().getSteps().get(0);
		Assert.assertEquals("some echo in after section", echoAfter.getAttributes().get("name"));
	}
}
