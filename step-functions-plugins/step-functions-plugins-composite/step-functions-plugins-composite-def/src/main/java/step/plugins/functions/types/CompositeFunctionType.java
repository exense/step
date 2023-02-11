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

import java.util.HashMap;
import java.util.Map;

import step.core.AbstractContext;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.PlanType;
import step.core.plans.PlanTypeRegistry;
import step.core.plans.builder.PlanBuilder;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.grid.filemanager.FileVersionId;
import step.planbuilder.BaseArtefacts;
import step.plugins.functions.types.composite.ArtefactFunctionHandler;

public class CompositeFunctionType extends AbstractFunctionType<CompositeFunction> {

	protected FileVersionId handlerJar;

	private final ObjectHookRegistry objectHookRegistry;
	private final PlanTypeRegistry planTypeRegistry;

	public CompositeFunctionType(ObjectHookRegistry objectHookRegistry, PlanTypeRegistry planTypeRegistry) {
		super();
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
			Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).endBlock().build();
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

			function.setPlan(plan);
		}
	}

	@Override
	public CompositeFunction copyFunction(CompositeFunction function) throws FunctionTypeException {
		CompositeFunction copy = super.copyFunction(function);

		// copy plan
		if (copy.getPlan() != null) {
			// TODO: probably it is not required to copy plan here
			PlanType<Plan> planType = planTypeRegistry != null ? (PlanType<Plan>) planTypeRegistry.getPlanType(copy.getPlan().getClass()) : null;
			if (planType != null) {
				copy.setPlan(planType.clonePlan(copy.getPlan(), false));
			} else {
				throw new FunctionTypeException("Unable to resolve plan type for class " + copy.getPlan().getClass());
			}
		}

		return copy;
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
