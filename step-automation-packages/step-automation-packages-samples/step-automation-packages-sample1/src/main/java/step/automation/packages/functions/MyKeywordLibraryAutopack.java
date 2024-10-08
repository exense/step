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
package step.automation.packages.functions;

import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Input;
import step.handlers.javahandler.Keyword;
import step.junit.runners.annotations.Plan;
import step.junit.runners.annotations.PlanCategories;
import step.junit.runners.annotations.Plans;

@Plans({"plan.plan"})
@PlanCategories({"PlainTextPlan", "AnnotatedPlan"})
public class MyKeywordLibraryAutopack extends AbstractKeyword {

	@Keyword
	public void MyKeyword2(@Input(name = "myInput", required = false, defaultValue = "defaultValueString") String myInput) {
		System.out.println("MyKeyword2 called!");
		output.add("MyKey", myInput);
		if (properties != null) {
			properties.forEach((key, value) -> {
				output.add(key, value);
			});
		}
	}

	@Plan("Echo PARAM_EXEC")
	@PlanCategories({"InlinePlan","AnnotatedPlan"})
	@Keyword(name = "Inline Plan")
	public void inlinePlan() {}

}
