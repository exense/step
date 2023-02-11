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
package step.plugins.functions.types.composite;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.BaseArtefactPlugin;
import step.artefacts.Return;
import step.artefacts.Script;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.reports.ErrorType;
import step.functions.handler.AbstractFunctionHandler;
import step.functions.handler.FunctionHandlerFactory;
import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.planbuilder.BaseArtefacts;

public class ArtefactFunctionHandlerTest {

	@Test
	public void test() {
		ExecutionContext context = newExecutionContext();

		Return r = new Return();
		r.setOutput(new DynamicValue<>("{\"Result\":{\"dynamic\":true,\"expression\":\"input.Input1\"}}"));
		
		Plan compositePlan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(r).endBlock().build();
		context.getPlanAccessor().save(compositePlan);
		
		ReportNode parentNode = new ReportNode();
		context.getReportNodeAccessor().save(parentNode);
		
		ArtefactFunctionHandler handler = createArtefactFunctionHandler(context);
		
		Input<JsonObject> input = new Input<>();
		input.setPayload(Json.createObjectBuilder().add("Input1", "InputValue1").build());
		Map<String, String> properties = getInputProperties(compositePlan, parentNode);
		input.setProperties(properties);
		Output<JsonObject> output = handler.handle(input);
		
		Assert.assertNull(output.getError());
		Assert.assertEquals("InputValue1", output.getPayload().getString("Result"));
		
		AtomicInteger count = new AtomicInteger(0);
		context.getReportNodeAccessor().getAll().forEachRemaining(n->count.incrementAndGet());
		Assert.assertEquals(3, count.get());
	}

	protected ExecutionContext newExecutionContext() {
		return ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).build().newExecutionContext();
	}

	protected ArtefactFunctionHandler createArtefactFunctionHandler(ExecutionContext context) {
		ArtefactFunctionHandler handler = new ArtefactFunctionHandler();
		
		FunctionHandlerFactory functionHandlerFactory = new FunctionHandlerFactory(new ApplicationContextBuilder(), null);
		
		TokenReservationSession tokenReservationSession = new TokenReservationSession();
		tokenReservationSession.put(AbstractFunctionHandler.EXECUTION_CONTEXT_KEY, context);
		
		functionHandlerFactory.initialize(handler, new TokenSession(), tokenReservationSession, null);
		return handler;
	}
	
	@Test
	public void testError() {
		ExecutionContext context = newExecutionContext();

		Script script = new Script();
		script.setScript("output.setError('MyError'");
		
		Plan compositePlan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(script).endBlock().build();
		context.getPlanAccessor().save(compositePlan);
		
		ReportNode parentNode = new ReportNode();
		context.getReportNodeAccessor().save(parentNode);
		
		ArtefactFunctionHandler handler = createArtefactFunctionHandler(context);
		
		Input<JsonObject> input = new Input<>();
		Map<String, String> properties = getInputProperties(compositePlan, parentNode);
		input.setProperties(properties);
		Output<JsonObject> output = handler.handle(input);
		
		Assert.assertEquals("Error in composite keyword", output.getError().getMsg());
		Assert.assertEquals(ErrorType.TECHNICAL, output.getError().getType());
	}

	private Map<String, String> getInputProperties(Plan compositePlan, ReportNode parentNode) {
		Map<String, String> properties = new HashMap<>();
		properties.put(ArtefactFunctionHandler.COMPOSITE_FUNCTION_KEY, compositePlan.getId().toString());
		properties.put(AbstractFunctionHandler.PARENTREPORTID_KEY, parentNode.getId().toString());
		return properties;
	}
}
