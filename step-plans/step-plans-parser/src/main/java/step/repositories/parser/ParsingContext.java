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
import java.util.Stack;

import ch.exense.commons.app.Configuration;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.PlanAccessor;
import step.functions.accessor.FunctionAccessor;

public class ParsingContext {
	
	private AbstractStep currentStep;
	
	private StepsParser stepParser;
	
	private final FunctionAccessor functionAccessor;
	
	private final PlanAccessor planAccessor;

	private Configuration configuration;
	
	private Stack<StackEntry> stack = new Stack<>();
	
	public ParsingContext(StepsParser stepParser, FunctionAccessor functionAccessor, PlanAccessor planAccessor, Configuration configuration) {
		super();
		this.stepParser = stepParser;
		this.functionAccessor = functionAccessor;
		this.planAccessor = planAccessor;
		this.configuration = configuration;
	}
	
	public ParsingContext(ParsingContext parent) {
		this.stack = parent.stack;
		this.stepParser = parent.stepParser;
		this.parsingErrors = parent.parsingErrors;
		this.functionAccessor = parent.functionAccessor;
		this.planAccessor = parent.planAccessor;
		this.configuration = parent.configuration;
	}

	public StepsParser getStepParser() {
		return stepParser;
	}

	public AbstractStep getCurrentStep() {
		return currentStep;
	}

	protected void setCurrentStep(AbstractStep currentStep) {
		this.currentStep = currentStep;
	}

	public void addArtefactToCurrentParent(AbstractArtefact artefact) {
		if(!stack.isEmpty()) {
			StackEntry entry = stack.peek();
			AbstractArtefact parent = entry.artefact;
			if(parent!=null) {
				parent.addChild(artefact);
			}				
		}
	}
	
	public void addArtefactToCurrentParentAndPush(AbstractArtefact artefact) {
		addArtefactToCurrentParent(artefact);
		pushArtefact(artefact);
	}
	
	public void pushArtefact(AbstractArtefact artefact) {
		stack.push(new StackEntry(currentStep, artefact));
	}
	
	protected StackEntry pop() {
		return stack.pop();
	}
	
	public AbstractArtefact popCurrentArtefact() {
		StackEntry entry = stack.pop();
		return entry.artefact;
	}
	
	public boolean isArtefactStackEmpty() {
		return stack.isEmpty();
	}
	
	protected StackEntry peek() {
		return stack.peek();
	}
	
	public AbstractArtefact peekCurrentArtefact() {
		StackEntry entry = stack.peek();
		return entry.artefact;
	}
	
	public void parseStep(AbstractStep step) {
		stepParser.parseStep(this, step);
	}
	
	public void parseStep(ParsingContext parsingContext, AbstractStep step) {
		stepParser.parseStep(parsingContext, step);
	}
	
	public FunctionAccessor getFunctionAccessor() {
		return functionAccessor;
	}
	
	public PlanAccessor getPlanAccessor() {
		return planAccessor;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	protected List<ParsingError> parsingErrors = new ArrayList<>();
	
	public static class ParsingError {
		
		AbstractStep step;
		
		String error;

		public ParsingError(AbstractStep step, String error) {
			super();
			this.step = step;
			this.error = error;
		}

		public AbstractStep getStep() {
			return step;
		}

		public String getError() {
			return error;
		}
	}
	
	public void addParsingError(String errorMsg) {
		String errorPrefix;
		if(currentStep!=null) {
			errorPrefix = "Error in step " + currentStep.toString() + ": ";
		} else {
			errorPrefix = "";
		}
		ParsingError error = new ParsingError(currentStep, errorPrefix+errorMsg);
		parsingErrors.add(error);
	}
	
	protected static class StackEntry {
		
		AbstractStep step;
		
		AbstractArtefact artefact;

		public StackEntry(AbstractStep step, AbstractArtefact artefact) {
			super();
			this.step = step;
			this.artefact = artefact;
		}
	}
}
