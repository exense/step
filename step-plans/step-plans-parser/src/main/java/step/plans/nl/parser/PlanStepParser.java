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

import java.util.HashMap;
import java.util.Map;

import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.repositories.parser.AbstractStep;
import step.repositories.parser.ParsingContext;
import step.repositories.parser.StepParser;
import step.repositories.parser.steps.DescriptionStep;
import step.repositories.parser.steps.ExpectedStep;

public class PlanStepParser implements StepParser<PlanStep> {

	@Override
	public int getParserScoreForStep(AbstractStep step) {
		return (step instanceof PlanStep)?1:0;
	}

	private static final String ASSERT_TOKEN = "Assert";
	
	@Override
	public void parseStep(ParsingContext parsingContext, PlanStep step) {
		AbstractStep subStep;
		if(step.command.trim().length()>0) {
			if(step.command.trim().startsWith(ASSERT_TOKEN)) {
				AbstractArtefact current = parsingContext.peekCurrentArtefact();
				subStep = new ExpectedStep(step.getCommand().replaceFirst(ASSERT_TOKEN, "").trim());
				// Expected block is parsed only if the current artefact is a CallFunction or if the last child of the current artefact is a CallFunction
				if(current!=null && current.getChildren()!=null && current.getChildren().size()>0) {
					AbstractArtefact lastChild = current.getChildren().get(current.getChildren().size()-1);
					if(lastChild instanceof CallFunction) {
						// the CallFunction is first pushed to the stack, so that the expected steps are added to the CallFunction as child
						parsingContext.pushArtefact(lastChild);				
						parsingContext.parseStep(newParsingContext(parsingContext, step), subStep);
						parsingContext.popCurrentArtefact();
					}
				} else if (current instanceof CallFunction) {
					parsingContext.parseStep(newParsingContext(parsingContext, step), subStep);
				} else {
					parsingContext.addParsingError(ASSERT_TOKEN+"s have to be used within or directly after a Keyword");
				}
			} else {
				subStep = new DescriptionStep(step.getCommand().trim());			
				parsingContext.parseStep(newParsingContext(parsingContext, step), subStep);
			}
		}
	}
	

	private ParsingContext newParsingContext(ParsingContext parsingContext, PlanStep step) {
		return new ParsingContext(parsingContext) {

			@Override
			public void addArtefactToCurrentParent(AbstractArtefact artefact) {
				String stepName = step.getName();
				String dynamicNameExpression = step.getDynamicNameExpression();
				if(stepName!=null) {
					artefact.setName(stepName);
				} else if(dynamicNameExpression != null) {
					artefact.setDynamicName(new DynamicValue<>(dynamicNameExpression, ""));
					artefact.setUseDynamicName(true);
				}
				artefact.setAttachments(step.getAttachments());
				artefact.setDescription(step.getDescription());
				artefact.addCustomAttribute("line", step.line);
				artefact.addCustomAttribute("source", step.command);
				super.addArtefactToCurrentParent(artefact);
			}
			
			public void addParsingError(String errorMsg) {
				String errorPrefix = "Error on line "+step.line+ ": ";
				parsingErrors.add(new ParsingError(step, errorPrefix + errorMsg));
			}
			
		};
	}

}
