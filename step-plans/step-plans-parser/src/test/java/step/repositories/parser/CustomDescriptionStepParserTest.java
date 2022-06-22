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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.artefacts.CallFunction;
import step.artefacts.Sequence;
import step.artefacts.Set;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.PlanAccessor;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.repositories.parser.StepsParser.ParsingException;
import step.repositories.parser.steps.DescriptionStep;

public class CustomDescriptionStepParserTest extends AbstractStepParserTest {

	DynamicJsonObjectResolver resolver;
	
	@Before
	public void setUp() {
		parser = StepsParser.builder().withStepParsers(new CustomDescriptionStepParser()).build();
		resolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(new ExpressionHandler()));
	}
	
	@Test
	public void testNoAttributes() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("KW1"));
		
		CallFunction f = parseAndGetUniqueChild(steps, CallFunction.class);
		Assert.assertEquals("{}",f.getArgument().get());
	}
	
	@Test
	public void testBasic() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("KW1 a = \"b\" c=\"d\""));
		
		CallFunction f = parseAndGetUniqueChild(steps, CallFunction.class);
		Assert.assertEquals("{\"a\":\"b\",\"c\":\"d\"}",evaluateFunctionArgument(f));
	}
	
	@Test
	public void testSpaces1() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("KW1 a  = \"b \"   \n 	 c=\"d\"    "));
		
		CallFunction f = parseAndGetUniqueChild(steps, CallFunction.class);
		Assert.assertEquals("{\"a\":\"b \",\"c\":\"d\"}",evaluateFunctionArgument(f));
	}
	
	@Test
	public void testInputKeysWithSpaces1() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("KW1 \"my parameter with spaces\" = \"b \"   \n 	 c=\"d\"    "));
		
		CallFunction f = parseAndGetUniqueChild(steps, CallFunction.class);
		Assert.assertEquals("{\"my parameter with spaces\":\"b \",\"c\":\"d\"}",evaluateFunctionArgument(f));
	}
	
	@Test
	public void testReproducer() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("TestKeyword Parameter1 = \"Value1\" \r\n  \u00A0Parameter2 = \"Value2\" \r\n"));
		
		CallFunction f = parseAndGetUniqueChild(steps, CallFunction.class);
		Assert.assertEquals("{\"Parameter1\":\"Value1\",\"Parameter2\":\"Value2\"}",evaluateFunctionArgument(f));
	}
	
	@Test
	public void testUTF8() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("KWö   à=\"b\" c =   \"${test} d \"  "));
		
		CallFunction f = parseAndGetUniqueChild(steps, CallFunction.class);
		Assert.assertEquals("{\"à\":\"b\",\"c\":\"value d \"}",evaluateFunctionArgument(f));
	}
	
	@Test
	public void testKWWithSpaces() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("\"My Keyword with spaces\" a = \"b\" c=\"d\""));
		
		CallFunction f = parseAndGetUniqueChild(steps, CallFunction.class);
		Assert.assertEquals("{\"a\":\"b\",\"c\":\"d\"}",evaluateFunctionArgument(f));
		Assert.assertEquals("{\"name\":\"My Keyword with spaces\"}",f.getFunction().getValue());
	}
	
	@Test
	public void testKWWithAppPrefix() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("MyApp.MyKeyword a = \"b\" c=\"d\""));
		
		CallFunction f = parseAndGetUniqueChild(steps, CallFunction.class);
		Assert.assertEquals("{\"a\":\"b\",\"c\":\"d\"}",evaluateFunctionArgument(f));
		JsonObject readObject = Json.createReader(new StringReader(f.getFunction().getValue())).readObject();
		Assert.assertEquals("MyKeyword",readObject.getString("name"));
		Assert.assertEquals("MyApp",readObject.getString("application"));
	}
	
	@Test
	public void testKWWithSpacesAndAppWithSpace() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("\"My App with space.My Keyword with spaces\""));
		
		CallFunction f = parseAndGetUniqueChild(steps, CallFunction.class);
		Assert.assertEquals("{}",evaluateFunctionArgument(f));
		JsonObject readObject = Json.createReader(new StringReader(f.getFunction().getValue())).readObject();
		Assert.assertEquals("My Keyword with spaces",readObject.getString("name"));
		Assert.assertEquals("My App with space",readObject.getString("application"));
	}
	
	@Test
	public void testSet() throws Exception {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Set a='b'"));
		
		Set f = parseAndGetUniqueChild(steps, Set.class);
		Assert.assertEquals("a",f.getKey().getValue());
		Assert.assertEquals("'b'",f.getValue().getExpression());
	}

	@Test
	public void testSetEscaping() throws Exception {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Set a=\" \\n \\r \\\"bla \\\\ bla\\\" \\$ \""));
		
		Set f = parseAndGetUniqueChild(steps, Set.class);
		Assert.assertEquals("a",f.getKey().getValue());
		Assert.assertEquals("\" \\n \\r \\\"bla \\\\ bla\\\" \\$ \"",f.getValue().getExpression());
	}
	
	@Test
	public void testMultipleSet() throws Exception {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Set a='b'\t c=\"d\"\u00A0e = 'f'\n"));
		
		List<AbstractArtefact> children = getChildren(parse(steps));
		
		Set set = (Set) children.get(0);
		Assert.assertEquals("a",set.getKey().getValue());
		Assert.assertEquals("'b'",set.getValue().getExpression());
		
		set = (Set) children.get(1);
		Assert.assertEquals("c",set.getKey().getValue());
		Assert.assertEquals("\"d\"",set.getValue().getExpression());
		
		set = (Set) children.get(2);
		Assert.assertEquals("e",set.getKey().getValue());
		Assert.assertEquals("'f'",set.getValue().getExpression());
	}
	
	@Test
	public void testSetWithExpression() throws Exception {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Set a=b.c d= e.getF() num=1"));
		
		List<AbstractArtefact> children = getChildren(parse(steps));
		Set set0 = (Set) children.get(0);
		Assert.assertEquals("a",set0.getKey().getValue());
		Assert.assertEquals("b.c",set0.getValue().getExpression());
		
		Set set1 = (Set) children.get(1);
		Assert.assertEquals("d",set1.getKey().getValue());
		Assert.assertEquals("e.getF()",set1.getValue().getExpression());
		
		Set set2 = (Set) children.get(2);
		Assert.assertEquals("num",set2.getKey().getValue());
		Assert.assertEquals("1",set2.getValue().getExpression());
	}
	
	Function function = null;
	
	@Test
	public void testFunctionDeclaration() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Function name=\"test\""));
		//steps.add(step("Echo \"test\""));
		steps.add(step("EndFunction"));
		
		FunctionAccessor repo = new InMemoryFunctionAccessorImpl();
		PlanAccessor planAccessor = new InMemoryPlanAccessor();
		Sequence root = new Sequence();
		parser.parseSteps(root, steps, planAccessor, repo);
		
		Map<String, String> attributes = new HashMap<>();
		attributes.put("name", "test");
		Assert.assertNotNull(repo.findByAttributes(attributes));;
	}
	

	private String evaluateFunctionArgument(CallFunction f) {
		Map<String, Object> b = new HashMap<>();
		b.put("test", "value");
		JsonObject o = resolver.evaluate(Json.createReader(new StringReader(f.getArgument().get())).readObject(), b);
		return o.toString();
	}
	
	private DescriptionStep step(String input) {
		DescriptionStep step = new DescriptionStep();
		step.setValue(input);
		return step;
	}
}
