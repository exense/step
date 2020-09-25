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

import step.artefacts.Check;
import step.core.artefacts.AbstractArtefact;
import step.repositories.parser.StepsParser.ParsingException;
import step.repositories.parser.annotated.DefaultExpectedStepParser;
import step.repositories.parser.steps.ExpectedStep;

public class DefaultExpectedStepParserTest extends AbstractStepParserTest {

	@Before
	public void setUp() {
		parser = StepsParser.builder().withStepParsers(new DefaultExpectedStepParser()).build();
	}
	
	@Test
	public void test() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Key1 is Value1 and Key2 is Value 2 and Key  3 is Value3"));
		
		AbstractArtefact root = parse(steps);
		Assert.assertEquals(3,getChildren(root).size());
		
		Check check1 = (Check) getChildren(root).get(0);
		Assert.assertEquals("'Value1'.equals(output.getString('Key1'))",check1.getExpression().getExpression());
		
		Check check2 = (Check) getChildren(root).get(1);
		Assert.assertEquals("'Value 2'.equals(output.getString('Key2'))",check2.getExpression().getExpression());
		
		Check check3 = (Check) getChildren(root).get(2);
		Assert.assertEquals("'Value3'.equals(output.getString('Key  3'))",check3.getExpression().getExpression());
	}
	
	private ExpectedStep step(String input) {
		ExpectedStep step = new ExpectedStep(input);
		return step;
	}
}
