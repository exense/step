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
package step.automation.packages.junit;

import org.bson.types.ObjectId;
import step.automation.packages.AutomationPackageFromClassLoaderProvider;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.kwlibrary.NoKeywordLibraryProvider;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.Artefact;
import step.core.execution.ExecutionEngine;
import step.core.plans.PlanFilter;
import step.core.plans.filters.*;
import step.junit.runner.StepClassParserResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JUnitPlansProvider {

    private final Class<?> testClass;

    public JUnitPlansProvider(Class<?> testClass) {
        this.testClass = testClass;
    }

    public List<StepClassParserResult> getTestPlans(ExecutionEngine executionEngine) {
        try {
            AutomationPackageManager automationPackageManager = executionEngine.getExecutionEngineContext().require(AutomationPackageManager.class);
            AutomationPackageFromClassLoaderProvider automationPackageProvider = new AutomationPackageFromClassLoaderProvider(testClass.getClassLoader());
            ObjectId automationPackageId = automationPackageManager.createOrUpdateAutomationPackage(
                    false, true, null, automationPackageProvider, null, null,
                    true, null, null, null, false, new NoKeywordLibraryProvider(), null,
                    false, true).getId();

            List<PlanFilter> planFilterList = new ArrayList<>();
            IncludePlans includePlans = testClass.getAnnotation(IncludePlans.class);
            if (includePlans != null && includePlans.value() != null) {
                planFilterList.add(new PlanByIncludedNamesFilter(Arrays.asList(includePlans.value())));
            }
            ExcludePlans excludePlans = testClass.getAnnotation(ExcludePlans.class);
            if (excludePlans != null && excludePlans.value() != null) {
                planFilterList.add(new PlanByExcludedNamesFilter(Arrays.asList(excludePlans.value())));
            }
            IncludePlanCategories includePlanCategories = testClass.getAnnotation(IncludePlanCategories.class);
            if (includePlanCategories != null && includePlanCategories.value() != null) {
                planFilterList.add(new PlanByIncludedCategoriesFilter(Arrays.asList(includePlanCategories.value())));
            }
            ExcludePlanCategories excludePlanCategories = testClass.getAnnotation(ExcludePlanCategories.class);
            if (excludePlanCategories != null && excludePlanCategories.value() != null) {
                planFilterList.add(new PlanByExcludedCategoriesFilter(Arrays.asList(excludePlanCategories.value())));
            }
            PlanMultiFilter planMultiFilter = new PlanMultiFilter(planFilterList);

            return automationPackageManager.getPackagePlans(automationPackageId)
                    .stream()
                    .filter(planMultiFilter::isSelected)
                    .filter(p -> p.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution())
                    .map(p -> new StepClassParserResult(p.getAttribute(AbstractOrganizableObject.NAME), p, null))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Cannot read test plans", e);
        }
    }

}
