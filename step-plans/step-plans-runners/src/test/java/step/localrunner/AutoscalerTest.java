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
package step.localrunner;

import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.ThreadGroup;
import step.artefacts.handlers.functions.AutoscalerExecutionPlugin;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.Function;
import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.threadpool.ThreadPoolPlugin;

import javax.json.JsonObject;
import java.util.List;
import java.util.Map;

public class AutoscalerTest {
	
	@Test
	public void test() throws Exception {
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>("1*1", ""));

		ThreadGroup threadGroup2 = new ThreadGroup();
		threadGroup2.setUsers(new DynamicValue<>(2));

		ThreadGroup threadGroup3 = new ThreadGroup();
		threadGroup3.setUsers(new DynamicValue<>("1*3", ""));

		ThreadGroup threadGroup4 = new ThreadGroup();
		threadGroup4.setUsers(new DynamicValue<>(4));

		Plan otherPlan = PlanBuilder.create()
				.startBlock(threadGroup3)
				.add(FunctionArtefacts.keyword("test"))
				.endBlock().build();
		otherPlan.addAttribute(AbstractOrganizableObject.NAME, "MyOtherPlan");

		CallFunction callFunction = FunctionArtefacts.keyword("test");
		//callFunction.setToken(new DynamicValue<>("{\"key1\":\"value1\"}"));
		Plan compositePlan = PlanBuilder.create()
				.startBlock(threadGroup4)
				.add(callFunction)
				.endBlock().build();

		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.testScenario())
					.startBlock(threadGroup)
						.startBlock(FunctionArtefacts.session())
							.add(FunctionArtefacts.keyword("test"))
							.add(FunctionArtefacts.keyword("test"))
						.endBlock()
					.endBlock()
					.startBlock(threadGroup2)
						.startBlock(FunctionArtefacts.session())
							.add(FunctionArtefacts.keyword("test"))
						.endBlock()
					.endBlock()
					.add(BaseArtefacts.callPlan(otherPlan.getId().toString()))
					.add(FunctionArtefacts.keyword("MyComposite"))
				.endBlock().build();
		plan.setSubPlans(List.of(otherPlan));

		CompositeFunction compositeFunction = new CompositeFunction();
		compositeFunction.addAttribute(AbstractOrganizableObject.NAME, "MyComposite");
		compositeFunction.setPlan(compositePlan);

		MyFunction function = new MyFunction();
		function.addAttribute(AbstractOrganizableObject.NAME, "test");

		plan.setFunctions(List.of(function, compositeFunction));

		ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new FunctionPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {
			@Override
			public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
				super.initializeExecutionContext(executionEngineContext, executionContext);
				FunctionTypeRegistry functionTypeRegistry = executionContext.require(FunctionTypeRegistry.class);
				functionTypeRegistry.registerFunctionType(new CompositeFunctionType(null));
				functionTypeRegistry.registerFunctionType(new MyFunctionType());
			}
		}).withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new AutoscalerExecutionPlugin()).build();
		executionEngine.execute(plan).printTree();
	}

	public static class MyFunction extends Function {

		@Override
		public boolean requiresLocalExecution() {
			return false;
		}
	}

	public static class MyFunctionType extends AbstractFunctionType<MyFunction> {

		@Override
		public String getHandlerChain(MyFunction function) {
			return MyFunctionHandler.class.getName();
		}

		@Override
		public Map<String, String> getHandlerProperties(MyFunction function, AbstractStepContext executionContext) {
			return null;
		}

		@Override
		public MyFunction newFunction() {
			return new MyFunction();
		}
	}

	public static class MyFunctionHandler extends JsonBasedFunctionHandler {

		@Override
		public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
			return new OutputBuilder().build();
		}
	}

	public static class MyKeyword extends AbstractKeyword {

		@Keyword
		public void test() {

		}
	}
}

