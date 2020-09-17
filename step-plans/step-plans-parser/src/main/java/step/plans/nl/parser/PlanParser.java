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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.exense.commons.app.Configuration;
import step.artefacts.Sequence;
import step.artefacts.TestCase;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.repositories.parser.StepsParser;
import step.repositories.parser.StepsParser.ParsingException;

/**
 * A parser for plans in plain-text format
 *
 */
public class PlanParser {

	protected boolean wrapInTestcase = true;
	
	protected Configuration configuration;
	
	public PlanParser() {
		this.configuration = new Configuration();
	}
	
	public PlanParser(Configuration configuration)  {
		this.configuration = configuration;
	}

	public boolean isWrapInTestcase() {
		return wrapInTestcase;
	}

	public void setWrapInTestcase(boolean wrapInTestcase) {
		this.wrapInTestcase = wrapInTestcase;
	}

	/**
	 * Parses a {@link Plan} in plain-text format
	 * 
	 * @param content the plan in plain-text
	 * @return the parsed {@link Plan}
	 * @throws ParsingException if a parsing error occurs
	 */
	public Plan parse(String content) throws ParsingException {
		return parse(new StringReader(content));
	}
	
	/**
	 * Parses a {@link Plan} in plain-text format from a stream
	 * 
	 * @param inputStream the {@link InputStream} to be read
	 * @return the parsed {@link Plan}
	 * @throws ParsingException if a parsing error occurs
	 */
	public Plan parse(InputStream inputStream) throws ParsingException {
		return parse(new InputStreamReader(inputStream));
	}
	
	private static final Pattern DYNAMIC_NAME_PATTERN = Pattern.compile("[ ]*\\|(.+?)\\|[ ]*");
	
	protected Plan parse(Reader contentReader) throws ParsingException {
		StepsParser stepsParser = StepsParser.builder().withConfiguration(configuration).withExtensionsFromClasspath()
				.withStepParsers(new PlanStepParser()).build();

		BufferedReader reader = new BufferedReader(contentReader);
		
		List<PlanStep> descriptionSteps = new ArrayList<>();
		String currentDescriptionStepStr = null;
		boolean inBlock = false;
		
		String stepName = null;
		String dynamicStepName = null;
		String comment = null;
		
		int lineNumber = 0;
		for(String currentLine:reader.lines().collect(Collectors.toList())) {
			lineNumber++;
			if(currentLine.trim().startsWith("//")) {
				String commentValue = currentLine.trim().replaceFirst("//", "");
				if(stepName == null && dynamicStepName == null) {
					Matcher matcher = DYNAMIC_NAME_PATTERN.matcher(commentValue);
					if(matcher.matches()) {
						dynamicStepName = matcher.group(1);
					} else {
						stepName = commentValue;
					}
				} else {
					if(comment == null) {
						comment = commentValue;
					} else {
						comment += "\n" + commentValue;
					}
				}		
			} else {
				if(inBlock) {
					if(currentLine.trim().endsWith("-")) {
						inBlock = false;
						currentDescriptionStepStr += currentLine.replaceAll("-[ \t]*$", "");
						descriptionSteps.add(new PlanStep(stepName, dynamicStepName, comment, currentDescriptionStepStr, Integer.toString(lineNumber)));
						comment = null;
						stepName = null;
						dynamicStepName = null;
					} else {
						currentDescriptionStepStr += " " + currentLine;
					}
				} else {
					if(currentLine.trim().startsWith("-")) {
						inBlock = true;
						currentDescriptionStepStr = currentLine.replaceFirst("-", "");
					} else {
						descriptionSteps.add(new PlanStep(stepName, dynamicStepName, comment, currentLine, Integer.toString(lineNumber)));
						comment = null;
						stepName = null;
						dynamicStepName = null;
					}
					
				}
				
			}
		}
		
		FunctionAccessor functionRepository = new InMemoryFunctionAccessorImpl();
		PlanAccessor planAccessor = new InMemoryPlanAccessor();
		
		AbstractArtefact wrapper = null;
		if(wrapInTestcase) {
			wrapper = new TestCase();
		} else {
			wrapper = new Sequence();
		}
		
//		List<DescriptionStep> steps = reader.lines().map(l-> new DescriptionStep(l)).collect(Collectors.toList());
		stepsParser.parseSteps(wrapper, descriptionSteps, planAccessor, functionRepository);

		Collection<Function> functions = new ArrayList<>();
		functionRepository.getAll().forEachRemaining(f->functions.add(f));
		
		Collection<Plan> subPlans = new ArrayList<>();
		planAccessor.getAll().forEachRemaining(p->subPlans.add(p));
		
		AbstractArtefact rootArtefact;
		if(wrapInTestcase) {
			rootArtefact = wrapper;
		} else {
			List<AbstractArtefact> children = wrapper.getChildren();
			if(children==null || children.size()==0) {
				throw new ParsingException("No root element found.");
			} else if (children.size()>1) {
				throw new ParsingException("More than one root element found. This is not allowed with the option wrapInTestcase=false");
			} else {
				rootArtefact = wrapper.getChildren().get(0);
			}
		}
		
		Plan plan = new Plan(rootArtefact);
		plan.setFunctions(functions);
		plan.setSubPlans(subPlans);
		return plan;
	}
}
