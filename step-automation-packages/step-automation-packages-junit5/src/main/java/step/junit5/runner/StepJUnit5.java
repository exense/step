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
package step.junit5.runner;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import step.automation.packages.AutomationPackageFromClassLoaderProvider;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.junit.*;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.Artefact;
import step.core.execution.ExecutionEngine;
import step.core.plans.PlanFilter;
import step.core.plans.filters.*;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.handlers.javahandler.AbstractKeyword;
import step.junit.runner.StepClassParserResult;
import step.junit.runners.annotations.ExecutionParameters;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class StepJUnit5 extends AbstractKeyword {

    private final static Pattern SYSTEM_PROPERTIES_PREFIX = Pattern.compile("STEP_(.+?)");

    @TestFactory
    public List<DynamicNode> plans() {
        ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new AbstractExecutionEnginePlugin(){}).withPluginsFromClasspath().build();

        List<StepClassParserResult> testPlans = getTestPlans(executionEngine);
        List<DynamicNode> tests = new ArrayList<>();
        for (StepClassParserResult testPlan : testPlans) {
            tests.add(DynamicTest.dynamicTest(testPlan.getName(), () -> new AbstractLocalPlanRunner(testPlan, executionEngine) {
                @Override
                protected void onExecutionStart() {
                    // TODO: extensions?
                }

                @Override
                protected void onExecutionError(PlanRunnerResult result, String errorText, boolean assertionError) {
                    // TODO: extensions?
                }

                @Override
                protected void onInitializingException(Exception exception) {
                    // TODO: extensions?
                }

                @Override
                protected void onExecutionException(Exception exception) {
                    // TODO: extensions?
                }

                @Override
                protected void onTestFinished() {
                    // TODO: extensions?
                }

                @Override
                protected Map<String, String> getExecutionParameters() {
                    return StepJUnit5.this.getExecutionParameters();
                }
            }.runPlan()));
        }
        return tests;
    }

    protected Map<String, String> getExecutionParameters() {
        HashMap<String, String> executionParameters = new HashMap<>();
        // Prio 3: Execution parameters from annotation ExecutionParameters
        executionParameters.putAll(getExecutionParametersByAnnotation());
        // Prio 2: Execution parameters from environment variables (prefixed with STEP_*)
        executionParameters.putAll(getExecutionParametersFromEnvironmentVariables());
        // Prio 3: Execution parameters from system properties
        executionParameters.putAll(getExecutionParametersFromSystemProperties());
        return executionParameters;
    }

    private Map<String, String> getExecutionParametersByAnnotation() {
        Map<String, String> executionParameters = new HashMap<>();
        ExecutionParameters params;
        if ((params = this.getClass().getAnnotation(ExecutionParameters.class)) != null) {
            String key = null;
            for (String param : params.value()) {
                if (key == null) {
                    key = param;
                } else {
                    executionParameters.put(key, param);
                    key = null;
                }
            }
        }
        return executionParameters;
    }

    protected Map<String, String> getExecutionParametersFromSystemProperties() {
        Map<String, String> executionParameters = new HashMap<>();
        System.getProperties().forEach((k, v) ->
                unescapeParameterKeyIfMatches(k.toString()).ifPresent(key -> executionParameters.put(key, v.toString())));
        return executionParameters;
    }

    private Map<String, String> getExecutionParametersFromEnvironmentVariables() {
        Map<String, String> executionParameters = new HashMap<>();
        System.getenv().forEach((k, v) -> unescapeParameterKeyIfMatches(k).ifPresent(key -> executionParameters.put(key, v)));
        return executionParameters;
    }

    private Optional<String> unescapeParameterKeyIfMatches(String key) {
        Matcher matcher = SYSTEM_PROPERTIES_PREFIX.matcher(key);
        if (matcher.matches()) {
            String unescapedKey = matcher.group(1);
            return Optional.of(unescapedKey);
        } else {
            return Optional.empty();
        }
    }

    // TODO: this code is copied from Step.class and be reused
    private List<StepClassParserResult> getTestPlans(ExecutionEngine executionEngine) {
        try {
            AutomationPackageManager automationPackageManager = executionEngine.getExecutionEngineContext().require(AutomationPackageManager.class);
            AutomationPackageFromClassLoaderProvider automationPackageProvider = new AutomationPackageFromClassLoaderProvider(getClass().getClassLoader());
            ObjectId automationPackageId = automationPackageManager.createOrUpdateAutomationPackage(
                    false, true, null, automationPackageProvider,
                    true, null, null, false
            ).getId();

            List<PlanFilter> planFilterList = new ArrayList<>();
            IncludePlans includePlans = getClass().getAnnotation(IncludePlans.class);
            if (includePlans != null && includePlans.value() != null) {
                planFilterList.add(new PlanByIncludedNamesFilter(Arrays.asList(includePlans.value())));
            }
            ExcludePlans excludePlans = getClass().getAnnotation(ExcludePlans.class);
            if (excludePlans != null && excludePlans.value() != null) {
                planFilterList.add(new PlanByExcludedNamesFilter(Arrays.asList(excludePlans.value())));
            }
            IncludePlanCategories includePlanCategories = getClass().getAnnotation(IncludePlanCategories.class);
            if (includePlanCategories != null && includePlanCategories.value() != null) {
                planFilterList.add(new PlanByIncludedCategoriesFilter(Arrays.asList(includePlanCategories.value())));
            }
            ExcludePlanCategories excludePlanCategories = getClass().getAnnotation(ExcludePlanCategories.class);
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
