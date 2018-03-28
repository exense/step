/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.artefacts.handlers;

import static org.junit.Assert.*;
import static step.planbuilder.FunctionPlanBuilder.keyword;

import org.junit.Test;

import step.artefacts.Assert;
import step.artefacts.Assert.AssertOperator;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.functions.runner.LocalRunner;
import step.planbuilder.PlanBuilder;

public class CallFunctionHandlerTest {
	
	@Test
	public void test() {
		Assert assertString = new Assert();
		assertString.setActual(new DynamicValue<String>("string"));
		assertString.setExpected(new DynamicValue<String>("string"));
		assertString.setOperator(AssertOperator.EQUALS);
		
		Assert assertInt = new Assert();
		assertInt.setActual(new DynamicValue<String>("int"));
		assertInt.setExpected(new DynamicValue<String>("1"));
		assertInt.setOperator(AssertOperator.EQUALS);

		Plan plan = PlanBuilder.create()
				.startBlock(keyword("keyword1", "{\"string\":\"string\",\"int\":1,\"array\":[],\"object\":{}}"))
				.add(assertString)
				.add(assertInt)
				.endBlock().build();
		
		LocalRunner runner = new LocalRunner(new EchoHandler());
		runner.run(plan);


		InMemoryReportNodeAccessor reportNodes = (InMemoryReportNodeAccessor) runner.getContext().getGlobalContext().getReportAccessor();
		
		ReportNode assertReportNode = reportNodes.getReportNodesByExecutionIDAndArtefactID(runner.getContext().getExecutionId(), assertString.getId().toString()).next();
		assertEquals(assertReportNode.getStatus(), ReportNodeStatus.PASSED);
		
		ReportNode assertIntReportNode = reportNodes.getReportNodesByExecutionIDAndArtefactID(runner.getContext().getExecutionId(), assertInt.getId().toString()).next();
		// Asserts of numbers and boolean are currently not implemented
		// assertEquals(assertIntReportNode.getStatus(), ReportNodeStatus.PASSED);
	}
}

