package step.plans.nl.parser;

import java.io.IOException;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.Echo;
import step.artefacts.ForEachBlock;
import step.artefacts.TestCase;
import step.core.plans.Plan;
import step.plans.nl.parser.PlanParser;
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
	public void testComments() throws IOException, ParsingException {
		PlanParser parser = new PlanParser();
		parser.setWrapInTestcase(false);
		Plan plan = parser.parse("//first comment is the name\n//this is a comment\nEcho 'test'");
		Assert.assertEquals(Echo.class, plan.getRoot().getClass());
		Assert.assertEquals("first comment is the name", plan.getRoot().getAttributes().get("name"));
		Assert.assertEquals("this is a comment", plan.getRoot().getDescription());
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
