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

import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.builder.PlanBuilder;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;
import step.grid.filemanager.FileVersionId;
import step.planbuilder.BaseArtefacts;
import step.plugins.functions.types.composite.ArtefactFunctionHandler;

public class CompositeFunctionType extends AbstractFunctionType<CompositeFunction> {
	
	protected FileVersionId handlerJar;
	
	protected final PlanAccessor planAccessor;
	
	public CompositeFunctionType(PlanAccessor planAccessor) {
		super();
		this.planAccessor = planAccessor;
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
		props.put(ArtefactFunctionHandler.PLANID_KEY, function.getPlanId());
		return props;
	}

	@Override
	public void setupFunction(CompositeFunction function) throws SetupFunctionException {
		super.setupFunction(function);
  		
  		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).endBlock().build();
  		// hide the plan of the composite keyword
  		plan.setVisible(false);

  		planAccessor.save(plan);
  		
  		function.setPlanId(plan.getId().toString());		
	}

	@Override
	public CompositeFunction copyFunction(CompositeFunction function) throws FunctionTypeException {
		CompositeFunction copy = super.copyFunction(function);

		// TODO Copy plan
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
