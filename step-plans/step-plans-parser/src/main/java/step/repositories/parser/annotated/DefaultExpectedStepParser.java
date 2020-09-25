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
package step.repositories.parser.annotated;

import step.artefacts.Check;
import step.core.dynamicbeans.DynamicValue;
import step.repositories.parser.ParsingContext;
import step.repositories.parser.steps.ExpectedStep;

public class DefaultExpectedStepParser extends AnnotatedStepParser<ExpectedStep> {

	public DefaultExpectedStepParser() {
		super(ExpectedStep.class);
		score = 100;
	}

	@Step(value = "(.+?)[ ]+and[ ]+(.+?)", priority = 10)
	public static void and(ParsingContext parsingContext, String left, String right) {
		ExpectedStep leftStep = new ExpectedStep(left);
		ExpectedStep rightStep = new ExpectedStep(right);
		parsingContext.parseStep(leftStep);
		parsingContext.parseStep(rightStep);
	}
	
	@Step("(.+?)[ ]+is[ ]+(.+?)")
	public static void map(ParsingContext parsingContext, String key, String value) {
		Check result = new Check();
		result.setExpression(new DynamicValue<>("'"+value+"'.equals(output.getString('"+key+"'))",""));
		parsingContext.addArtefactToCurrentParent(result);
	}
	
}
