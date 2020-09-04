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
		Assert.assertTrue(check1.isNegate());
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
		Assert.assertTrue(check1.isNegate());
	}
	
	@Test
	public void testOps() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 contains \"Value1\""));
		
		AbstractArtefact root = parse(steps);
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		Assert.assertEquals("\"Value1\"",check1.getExpected().getExpression());
		Assert.assertEquals("Key1",check1.getActual().get());
		Assert.assertEquals(AssertOperator.CONTAINS,check1.getOperator());
		Assert.assertFalse(check1.isNegate());
	}
	
	@Test
	public void testBegins() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 beginsWith \"Value1\""));
		
		AbstractArtefact root = parse(steps);
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		Assert.assertEquals("\"Value1\"",check1.getExpected().getExpression());
		Assert.assertEquals("Key1",check1.getActual().get());
		Assert.assertEquals(AssertOperator.BEGINS_WITH,check1.getOperator());
		Assert.assertFalse(check1.isNegate());
	}
	
	@Test
	public void testJsonPath() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("$.key1['key2'] beginsWith \"Value1\""));
		
		AbstractArtefact root = parse(steps);
		step.artefacts.Assert check1 = (step.artefacts.Assert ) getChildren(root).get(0);
		Assert.assertEquals("\"Value1\"",check1.getExpected().getExpression());
		Assert.assertEquals("$.key1['key2']",check1.getActual().get());
		Assert.assertEquals(AssertOperator.BEGINS_WITH,check1.getOperator());
		Assert.assertFalse(check1.isNegate());
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
		Assert.assertTrue(check2.isNegate());
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
