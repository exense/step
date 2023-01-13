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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.runner.Runner;
import step.core.artefacts.AbstractArtefact;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.handlers.javahandler.Keyword;
import step.junit.runners.annotations.ExecutionParameters;
import step.junit.runners.annotations.Plans;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;

public class StepClassParser {

    private final boolean appendClassnameToPlanName;
    private final PlanParser planParser = new PlanParser();

    public StepClassParser(boolean appendClassnameToPlanName) {
        super();
        this.appendClassnameToPlanName = appendClassnameToPlanName;
    }

    protected Map<String, String> getExecutionParametersForClass(Class<?> klass) {
        Map<String, String> executionParameters = new HashMap<String, String>();
        ExecutionParameters params;
        if ((params = klass.getAnnotation(ExecutionParameters.class)) != null) {
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

    public List<Runner> createRunnersForClass(Class<?> klass, ExecutionEngine executionEngine, Map<String, String> executionParameters) {
        final List<Runner> result = new ArrayList<>();
        // Plans from annotation @Plans
        result.addAll(getRunnersFromPlansAnnotation(klass, executionEngine));
        // Plans from methods annotated with @Plan
        result.addAll(getRunnersFromAnnotatedMethods(klass, executionEngine));
        return result;
    }

    private List<StepPlanRunner> getRunnersFromPlansAnnotation(Class<?> klass, ExecutionEngine executionEngine) {
        final List<StepPlanRunner> result = new ArrayList<>();
        Plans plans;
        if ((plans = klass.getAnnotation(Plans.class)) != null) {
            for (String name : plans.value()) {
                result.add(createPlanRunner(klass, name, executionEngine));
            }
        }
        return result;
    }

    private List<StepPlanRunner> getRunnersFromAnnotatedMethods(Class<?> klass, ExecutionEngine executionEngine) {
        try (AnnotationScanner annotationScanner = AnnotationScanner.forAllClassesFromClassLoader(klass.getClassLoader())) {
            return annotationScanner.getMethodsWithAnnotation(step.junit.runners.annotations.Plan.class).stream()
                    .filter(m -> m.getDeclaringClass() == klass).map(m -> {
                        String planName = (appendClassnameToPlanName ? m.getDeclaringClass().getName() + "." : "") + m.getName();
                        Exception exception = null;
                        Plan plan = null;
                        try {
                            String planStr = m.getAnnotation(step.junit.runners.annotations.Plan.class).value();
                            if (planStr.trim().length() == 0) {
                                Keyword keyword = m.getAnnotation(Keyword.class);
                                if (keyword != null) {
                                    String name = keyword.name();
                                    if (name.trim().length() > 0) {
                                        planStr = "\"" + name + "\"";
                                    } else {
                                        planStr = m.getName();
                                    }
                                } else {
                                    throw new IllegalStateException("Missing annotation @Keyword on implicit plan method " + m.getName());
                                }
                            }
                            plan = planParser.parse(planStr, RootArtefactType.TestCase);
                            setPlanName(plan, planName);
                        } catch (Exception e) {
                            exception = e;
                        }
                        try {
                            return new StepPlanRunner(klass, plan,planName, executionEngine, getExecutionParametersForClass(klass));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());
        }
    }

    private StepPlanRunner createPlanRunner(Class<?> klass, String name, ExecutionEngine executionEngine) {
        Plan plan = null;
        Exception exception = null;
        try {
            InputStream stream = klass.getResourceAsStream(name);
            if (stream == null) {
                throw new Exception("Plan '" + name + "' was not found for class " + klass.getName());
            }

            plan = planParser.parse(stream, RootArtefactType.TestCase);
            setPlanName(plan, name);
        } catch (Exception e) {
            exception = e;
        }

        return new StepPlanRunner(klass, plan, name, executionEngine, getExecutionParametersForClass(klass));
    }

    private static void setPlanName(Plan plan, String name) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(AbstractArtefact.NAME, name);
        plan.setAttributes(attributes);
        plan.getRoot().getAttributes().put(AbstractArtefact.NAME, name);
    }
}
