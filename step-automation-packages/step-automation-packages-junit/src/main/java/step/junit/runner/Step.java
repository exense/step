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
package step.junit.runner;

import org.bson.types.ObjectId;
import org.junit.runners.model.InitializationError;
import step.automation.packages.AutomationPackageFromClassLoaderProvider;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.junit.AutomationPackagePlans;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.Artefact;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Step extends AbstractStepRunner {

	public Step(Class<?> klass) throws InitializationError {
		super(klass, klass);

		try {
			executionEngine = ExecutionEngine.builder().withPlugin(new AbstractExecutionEnginePlugin() {
				@Override
				public void afterExecutionEnd(ExecutionContext context) {
					resourceManager = context.getResourceManager();
				}
			}).withPluginsFromClasspath().build();

			AutomationPackageManager automationPackageManager = executionEngine.getExecutionEngineContext().require(AutomationPackageManager.class);
			AutomationPackageFromClassLoaderProvider automationPackageProvider = new AutomationPackageFromClassLoaderProvider(this.klass.getClassLoader());
			ObjectId automationPackageId = automationPackageManager.createOrUpdateAutomationPackage(
					false, true, null, automationPackageProvider,
					true, null, null
			).getId();

			Predicate<Plan> planNameFilter = plan -> true;
			AutomationPackagePlans plansAnnotation = klass.getAnnotation(AutomationPackagePlans.class);
			if(plansAnnotation != null){
				String[] planNames = plansAnnotation.value();
				if(planNames != null && planNames.length != 0){
					planNameFilter = plan -> Arrays.asList(planNames).contains(plan.getAttribute(AbstractOrganizableObject.NAME));
				}
			}

			listPlans = automationPackageManager.getPackagePlans(automationPackageId)
					.stream()
					.filter(planNameFilter)
					.filter(p -> p.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution())
					.map(p -> new StepClassParserResult(p.getAttribute(AbstractOrganizableObject.NAME), p, null))
					.collect(Collectors.toList());
		} catch (Exception e) {
			throw new InitializationError(e);
		}
	}

}
