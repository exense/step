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

import static step.core.scanner.Classes.newInstanceAs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ch.exense.commons.app.Configuration;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.PlanAccessor;
import step.core.scanner.CachedAnnotationScanner;
import step.functions.accessor.FunctionAccessor;
import step.repositories.parser.ParsingContext.ParsingError;
import step.repositories.parser.ParsingContext.StackEntry;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class StepsParser {
	
	private final List<StepParser> parsers;
	private final Configuration configuration;
	
	private StepsParser(Configuration configuration, List<StepParser> parsers) {
		this.configuration = configuration;
		this.parsers = parsers;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {

		private Configuration configuration;
		private final List<StepParser> parsers = new ArrayList<>();
		
		public Builder withConfiguration(Configuration configuration) {
			this.configuration = configuration;
			return this;
		}
		
		public Builder withExtensionsFromClasspath() {
			CachedAnnotationScanner.getClassesWithAnnotation(StepParserExtension.class).stream()
				.map(newInstanceAs(StepParser.class)).forEach(parsers::add);
			return this;
		}
		
		public Builder withStepParsers(StepParser... stepsParser) {
			Arrays.asList(stepsParser).forEach(parsers::add);
			return this;
		}
		
		public StepsParser build() {
			if(configuration == null) {
				configuration = new Configuration();
			}
			return new StepsParser(configuration, parsers);
		}
	}

	public static class ParsingException extends Exception {
		
		private static final long serialVersionUID = 1L;
		
		List<ParsingError> errors;

		public ParsingException(String message) {
			super(message);
			errors = new ArrayList<>();
		}

		public ParsingException(List<ParsingError> errors) {
			super(errors.stream().map(e->e.error).collect(Collectors.joining("; ")));
			this.errors = errors;
		}

		public List<ParsingError> getErrors() {
			return errors;
		}

		public void setErrors(List<ParsingError> errors) {
			this.errors = errors;
		}
	}
	
	public void parseSteps(AbstractArtefact root, List<? extends AbstractStep> steps, PlanAccessor planAccessor, FunctionAccessor functionAccessor) throws ParsingException {
		ParsingContext parsingContext = new ParsingContext(this, functionAccessor, planAccessor, configuration);
		parsingContext.addArtefactToCurrentParentAndPush(root);
		
		for(AbstractStep step:steps) {
			parseStep(parsingContext, step);
		}

		if(!parsingContext.isArtefactStackEmpty()) {
			StackEntry stackEntry = parsingContext.pop();
			if(!parsingContext.isArtefactStackEmpty()) {
				parsingContext.addParsingError("Unclosed block " + stackEntry.step);
			}			
		} else {
			parsingContext.addParsingError("Unbalanced blocks");
		}
		
		if(parsingContext.parsingErrors.size()>0) {
			throw new ParsingException(parsingContext.parsingErrors);
		}		
	}
	
	public void parseSteps(AbstractArtefact root, List<? extends AbstractStep> steps) throws ParsingException {
		parseSteps(root, steps, null, null);
	}
	
	protected void parseStep(ParsingContext parsingContext, AbstractStep step) {
		parsingContext.setCurrentStep(step);

		int bestScore = 0;
		StepParser bestParser = null;
		for(StepParser parser:parsers) {
			int score = parser.getParserScoreForStep(step);
			if(score>bestScore) {
				bestScore = score;
				bestParser = parser;
			}
		}
		
		if(bestParser!=null) {
			bestParser.parseStep(parsingContext, step);
		} else {
			parsingContext.addParsingError("No parser found for "+ step.toString());
			//throw new RuntimeException("Unable to find parser for step: "+ step.toString());
		}
	}
}
