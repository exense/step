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

import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plugins.Plugin;
import step.core.scanner.CachedAnnotationScanner;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.handlers.javahandler.Keyword;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Set;

@Plugin(dependencies= {FunctionPlugin.class})
public class LocalCompositeFunctionPlugin extends AbstractExecutionEnginePlugin {

	private FunctionAccessor functionAccessor;
	private FunctionTypeRegistry functionTypeRegistry;
	private PlanAccessor planAccessor;

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		if (context.getOperationMode() == OperationMode.LOCAL) {
			functionAccessor = context.require(FunctionAccessor.class);
			planAccessor = context.getPlanAccessor();
			functionTypeRegistry = context.require(FunctionTypeRegistry.class);

			functionTypeRegistry.registerFunctionType(
					new CompositeFunctionType(
							planAccessor,
							context.inheritFromParentOrComputeIfAbsent(parentContext, ObjectHookRegistry.class, objectHookRegistryClass -> new ObjectHookRegistry()),
							null
					)
			);
			saveLocalFunctions();
		}
	}

	public void saveLocalFunctions() {
		Set<Method> methods = CachedAnnotationScanner.getMethodsWithAnnotation(Keyword.class);
		for (Method m : methods) {
			Keyword annotation = m.getAnnotation(Keyword.class);

			// keywords with plan reference are not local functions but composite functions linked with plan
			if (annotation.planReference() != null && !annotation.planReference().isBlank()) {
				try {
					Plan plan = planAccessor.save(parsePlanFromPlanReference(m, annotation.planReference()));
					functionAccessor.save(CompositeFunctionUtils.createLocalCompositeFunction(annotation, m, plan));
				} catch (Exception ex) {
					throw new RuntimeException("Unable to prepare local composite", ex);
				}
			}
		}
	}

	private Plan parsePlanFromPlanReference(Method m, String planReference) throws Exception {
		InputStream stream = m.getDeclaringClass().getResourceAsStream(planReference);
		if (stream == null) {
			throw new Exception("Plan '" + planReference + "' was not found for class " + m.getClass().getName());
		}

		Plan plan = new PlanParser().parse(stream, RootArtefactType.TestCase);
		plan.setVisible(false);
		return plan;
	}

}