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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.handlers.javahandler.Keyword;
import step.junit.runners.annotations.Plans;
import step.plans.nl.parser.PlanParser;

public class StepClassParser {

	private static final String DEFAULT_PLAN_EXTENTION = ".plan";

	private final PlanParser planParser = new PlanParser();

	public List<StepClassParserResult> createPlansForClass(Class<?> klass) throws Exception {
		final List<StepClassParserResult> result = new ArrayList<>();

		Plans plans;
		if ((plans = klass.getAnnotation(Plans.class)) != null) {
			// 1. case: explicit list of plans:
			for (String name : plans.value()) {
				result.add(createPlan(klass, name));
			}
		} else {
			// 2. case: all *.plans files in the package:
			result.addAll(getPlansForClass(klass));
		}

		result.addAll(getPlanFromAnnotatedMethods(klass));
		return result;
	}

	protected List<StepClassParserResult> getPlanFromAnnotatedMethods(Class<?> klass) {
		return AnnotationScanner.getMethodsWithAnnotation(step.junit.runners.annotations.Plan.class).stream()
				.filter(m -> m.getDeclaringClass() == klass).map(m -> {
					String planName = m.getName();
					Exception exception = null;
					Plan plan = null;
					try {
						String planStr = m.getAnnotation(step.junit.runners.annotations.Plan.class).value();
						if (planStr == null || planStr.trim().length() == 0) {
							Keyword keyword = m.getAnnotation(Keyword.class);
							if (keyword != null) {
								String name = keyword.name();
								if (name != null && name.trim().length() > 0) {
									planStr = name;
								} else {
									planStr = m.getName();
								}
							} else {
								planStr = m.getName();
							}
						}
						plan = planParser.parse(planStr);
						setPlanName(plan, planName);
					} catch (Exception e) {
						exception = e;
					}
					return new StepClassParserResult(planName, plan, exception);
				}).collect(Collectors.toList());
	}

	protected List<StepClassParserResult> getPlansForClass(Class<?> klass) throws Exception {
		List<StepClassParserResult> result = new ArrayList<>();

		URL url = klass.getResource(".");

		File folder = new File(url.getFile());
		for (File f : folder.listFiles()) {
			if (f.getName().endsWith(DEFAULT_PLAN_EXTENTION)) {
				result.add(createPlan(klass, f.getName()));
			}
		}
		return result;
	}

	protected StepClassParserResult createPlan(Class<?> klass, String name) throws Exception {
		Plan plan = null;
		Exception exception = null;
		try {
			InputStream stream = klass.getResourceAsStream(name);
			if (stream == null) {
				throw new Exception("Plan '" + name + "' was not found for class " + klass.getName());
			}

			plan = planParser.parse(stream);
			setPlanName(plan, name);
		} catch (Exception e) {
			exception = e;
		}

		return new StepClassParserResult(name, plan, exception);
	}

	public static void setPlanName(Plan plan, String name) {
		Map<String, String> attributes = new HashMap<>();
		attributes.put(AbstractArtefact.NAME, name);
		plan.setAttributes(attributes);
		plan.getRoot().getAttributes().put(AbstractArtefact.NAME, name);
	}
}
