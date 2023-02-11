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
package step.plugins.functions.types;

import step.core.AbstractContext;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.PlanTypeRegistry;
import step.core.plans.builder.PlanBuilder;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.grid.filemanager.FileVersionId;
import step.planbuilder.BaseArtefacts;
import step.plugins.functions.types.composite.ArtefactFunctionHandler;

import java.util.HashMap;
import java.util.Map;

public class CompositeFunctionType extends AbstractFunctionType<CompositeFunction> {

	protected FileVersionId handlerJar;

	private final PlanAccessor planAccessor;
	private final ObjectHookRegistry objectHookRegistry;
	private final PlanTypeRegistry planTypeRegistry;

	public CompositeFunctionType(PlanAccessor planAccessor, ObjectHookRegistry objectHookRegistry, PlanTypeRegistry planTypeRegistry) {
		super();
		this.planAccessor = planAccessor;
		this.objectHookRegistry = objectHookRegistry;
		this.planTypeRegistry = planTypeRegistry;
	}

	@Override
	public void init() {
		super.init();
		handlerJar = registerResource(getClass().getClassLoader(), "step-functions-composite-handler.jar", false);
	}

	@Override
	public String getHandlerChain(CompositeFunction function) {
		return ArtefactFunctionHandler.class.getName();
	}

	@Override
	public Map<String, String> getHandlerProperties(CompositeFunction function) {
		Map<String, String> props = new HashMap<>();
		props.put(ArtefactFunctionHandler.COMPOSITE_FUNCTION_KEY, function.getId().toString());
		return props;
	}

	@Override
	public void setupFunction(CompositeFunction function) throws SetupFunctionException {
		super.setupFunction(function);

		// if existing plan is not explicitly selected for the new function, we create a new one
		if (function.getPlan() == null) {
			Plan plan = null;

			if (function.getPlanId() == null) {
				// create a new blank plan
				plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).endBlock().build();
			} else {
				// TODO: maybe we need to change this behavior
				// if we create a new composite with linked existing plan (for example, in step.controller.grid.services.FunctionServices)
				// the plan id is passed in composite function - we need to store the copy of this plan inside the composite
				plan = planAccessor.get(function.getPlanId());
				if (plan == null) {
					throw new SetupFunctionException("Plan not found by id: " + function.getPlanId());
				}

				// we only use planId field to pass the plan when create a new function, but we don't need to store the id
				function.setPlanId(null);
			}

			// hide the plan of the composite keyword
			plan.setVisible(false);

			// add same context attributes to plan
			if (objectHookRegistry != null) {
				try {
					AbstractContext context = new AbstractContext() {
					};
					objectHookRegistry.rebuildContext(context, function);
					objectHookRegistry.getObjectEnricher(context).accept(plan);
				} catch (Exception e) {
					throw new SetupFunctionException("Error while rebuilding context for function " + function, e);
				}
			}

			// save plan in composite function
			function.setPlan(plan);
		}
	}

	@Override
	public CompositeFunction copyFunction(CompositeFunction function) throws FunctionTypeException {
		return super.copyFunction(function);
	}

	@Override
	public FileVersionId getHandlerPackage(CompositeFunction function) {
		return handlerJar;
	}

	@Override
	public CompositeFunction newFunction() {
		return new CompositeFunction();
	}
}
