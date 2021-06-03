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

import step.artefacts.Echo;
import step.artefacts.ForBlock;
import step.artefacts.Sequence;
import step.repositories.parser.StepsParser.ParsingException;

public class StepsParserTest extends AbstractStepParserTest {
	
	@Before
	public void setUp() {
		parser = StepsParser.builder().withStepParsers(new StepParser<AbstractStep>() {
			
			@Override
			public int getParserScoreForStep(AbstractStep step) {
				String stepName = step.getName();
				return (stepName.equals("Start")||stepName.equals("End")||stepName.equals("Echo")
						||stepName.equals("Other Echo")||stepName.equals("Call Echo"))?1:0;
			}
			
			@Override
			public void parseStep(ParsingContext parsingContext, AbstractStep step) {
				if(step.getName().equals("Start")) {
					parsingContext.addArtefactToCurrentParentAndPush(new ForBlock());
				} else if(step.getName().equals("End")) {
					parsingContext.popCurrentArtefact();
				} else if(step.getName().equals("Echo")) {
					parsingContext.addArtefactToCurrentParent(new Echo());
				} else if(step.getName().equals("Other Echo")) {
					parsingContext.addArtefactToCurrentParent(new Echo());
				} else if(step.getName().equals("Call Echo")) {
					parsingContext.parseStep(step("Echo 2"));
				} 
			}
		}).withStepParsers(new StepParser<AbstractStep>() {
			
			@Override
			public int getParserScoreForStep(AbstractStep step) {
				return step.name.equals("Other Echo")?10:0;
			}
			
			@Override
			public void parseStep(ParsingContext parsingContext, AbstractStep step) {
				Echo echo = new Echo();
				echo.setDescription("Echo 2");
				parsingContext.addArtefactToCurrentParent(echo);
			}
		}).build();
	}
	
	@Test
	public void test() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Start"));
		steps.add(step("Echo"));
		steps.add(step("End"));
		
		Sequence root = parse(steps);

		Assert.assertEquals(1,getChildren(root).size());
		Assert.assertEquals(1,getChildren(getChildren( root).get(0)).size());
	}
	
	@Test
	public void test2() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Start"));
		steps.add(step("Echo"));
		steps.add(step("Echo"));
		steps.add(step("End"));
		
		Sequence root = parse(steps);

		Assert.assertEquals(1,getChildren(root).size());
		Assert.assertEquals(2,getChildren(getChildren( root).get(0)).size());
	}
	
	@Test
	public void test3() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Start"));
		steps.add(step("Echo"));
		steps.add(step("Echo"));
		steps.add(step("End"));
		steps.add(step("Start"));
		steps.add(step("Echo"));
		steps.add(step("End"));
		
		Sequence root = parse(steps);

		Assert.assertEquals(2,getChildren( root).size());
		Assert.assertEquals(2,getChildren(getChildren( root).get(0)).size());
		Assert.assertEquals(1,getChildren(getChildren( root).get(1)).size());
	}
	
	@Test
	public void testReParse() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Start"));
		steps.add(step("Other Echo"));
		steps.add(step("End"));
		
		Sequence root = parse(steps);

		Assert.assertEquals(1,getChildren( root).size());
		Echo echo = (Echo) getChildren(getChildren( root).get(0)).get(0);
		Assert.assertEquals("Echo 2",echo.getDescription());
		
	}
	
	@Test
	public void testParserPriority() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Start"));
		steps.add(step("Other Echo"));
		steps.add(step("End"));
		
		Sequence root = parse(steps);

		Assert.assertEquals(1,getChildren( root).size());
		Echo echo = (Echo) getChildren(getChildren( root).get(0)).get(0);
		Assert.assertEquals("Echo 2",echo.getDescription());
		
	}


	@Test
	public void testUnclosedBlockError() {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Start"));
		steps.add(step("Echo"));

		ParsingException exception = null;
		try {
			parse(steps);
		} catch(ParsingException e) {
			exception = e;
		}

		Assert.assertNotNull(exception);
		Assert.assertEquals(1 ,exception.getErrors().size());
		Assert.assertEquals("Error in step Echo: Unclosed block Start",exception.getErrors().get(0).getError());
	}

	@Test
	public void testUnclosedBlocksError() {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Start"));
		steps.add(step("Start"));
		steps.add(step("Echo"));

		ParsingException exception = null;
		try {
			parse(steps);
		} catch(ParsingException e) {
			exception = e;
		}

		Assert.assertNotNull(exception);
		Assert.assertEquals(2 ,exception.getErrors().size());
		Assert.assertEquals("Error in step Echo: Unclosed block Start",exception.getErrors().get(0).getError());
		Assert.assertEquals("Error in step Echo: Unclosed block Start",exception.getErrors().get(1).getError());
	}

	@Test
	public void testTrailingEndError() {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Echo"));
		steps.add(step("End"));

		ParsingException exception = null;
		try {
			parse(steps);
		} catch(ParsingException e) {
			exception = e;
		}

		Assert.assertNotNull(exception);
		Assert.assertEquals(1 ,exception.getErrors().size());
		Assert.assertEquals("Error in step End: Unbalanced blocks",exception.getErrors().get(0).getError());
	}

	@Test
	public void testParsingError() {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Start"));
		steps.add(step("No step"));
		steps.add(step("End"));
		
		ParsingException exception = null;
		try {
			parse(steps);			
		} catch(ParsingException e) {
			exception = e;
		}

		Assert.assertNotNull(exception);
		Assert.assertEquals("Error in step No step: No parser found for No step",exception.getErrors().get(0).getError());
	}

	private AbstractStep step(String input) {
		AbstractStep step = new AbstractStep();
		step.setName(input);
		return step;
	}
}
