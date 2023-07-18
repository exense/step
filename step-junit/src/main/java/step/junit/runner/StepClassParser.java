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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.handlers.javahandler.Keyword;
import step.junit.runners.annotations.Plans;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;
import step.plans.simple.YamlPlanSerializer;
import step.plans.simple.model.SimpleYamlPlanVersions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StepClassParser {
	private static final Logger logger = LoggerFactory.getLogger(StepClassParser.class);

	private final boolean appendClassnameToPlanName;

	private final PlanParser planParser = new PlanParser();

	private final YamlPlanSerializer simpleYamlPlanSerializer;

	public StepClassParser(boolean appendClassnameToPlanName) {
		super();
		this.appendClassnameToPlanName = appendClassnameToPlanName;
		this.simpleYamlPlanSerializer = new YamlPlanSerializer(SimpleYamlPlanVersions.ACTUAL_VERSION);
	}

	public List<StepClassParserResult> createPlansForClass(Class<?> klass) throws Exception {
		final List<StepClassParserResult> result = new ArrayList<>();
		// Plans from annotation @Plans
		result.addAll(getPlansFromPlansAnnotation(klass));
		// Plans from methods annotated with @Plan
		result.addAll(getPlanFromAnnotatedMethods(klass));
		return result;
	}

	public List<StepClassParserResult> getPlansFromPlansAnnotation(Class<?> klass) throws Exception {
		final List<StepClassParserResult> result = new ArrayList<>();
		Plans plans;

		logger.debug("searching for annotation for class "+klass.getName());
		if ((plans = klass.getAnnotation(Plans.class)) != null) {
			logger.debug("found annotation :"+ plans.value() + " for class "+klass.getName());
			for (String name : plans.value()) {
				result.add(createPlan(klass, name));
			}
		}
		return result;
	}

	public List<StepClassParserResult> getPlanFromAnnotatedMethods(AnnotationScanner annotationScanner, Class<?> klass) {
		return annotationScanner.getMethodsWithAnnotation(step.junit.runners.annotations.Plan.class).stream()
			.filter(m -> m.getDeclaringClass() == klass).map(m -> {
				String planName = (appendClassnameToPlanName?m.getDeclaringClass().getName()+".":"")+m.getName();
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
							throw new IllegalStateException("Missing annotation @Keyword on implicit plan method "+m.getName());
						}
					}
					plan = planParser.parse(planStr, RootArtefactType.TestCase);
					setPlanName(plan, planName);
				} catch (Exception e) {
					exception = e;
				}
				return new StepClassParserResult(planName, plan, exception);
			}).collect(Collectors.toList());
	}

	public List<StepClassParserResult> getPlanFromAnnotatedMethods(Class<?> klass) {
		try (AnnotationScanner annotationScanner = AnnotationScanner.forAllClassesFromClassLoader(klass.getClassLoader())) {
			return getPlanFromAnnotatedMethods(annotationScanner,klass);
		}
	}

	public StepClassParserResult createPlan(Class<?> klass, String name) throws Exception {
		Plan plan = null;
		Exception exception = null;
		try {
			ParserMode parserMode = chooseParserModeByFileName(name);

			InputStream stream = klass.getResourceAsStream(name);
			if (stream == null) {
				throw new Exception("Plan '" + name + "' was not found for class " + klass.getName());
			}

			if (parserMode == ParserMode.PLAN_PARSER) {
				plan = planParser.parse(stream, RootArtefactType.TestCase);
				logger.debug("plan is:" + plan + " for class " + klass.getName());
				setPlanName(plan, name);
			} else if (parserMode == ParserMode.SIMPLE_YAML_PARSER) {
				plan = simpleYamlPlanSerializer.readSimplePlanFromYaml(stream);
			} else {
				throw new UnsupportedOperationException("Unable to resolve plan parser " + parserMode);
			}
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

	private ParserMode chooseParserModeByFileName(String fileName) {
		// TODO: maybe add the 'format' property to @Plan annotation instead of checking file extension
		if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
			return ParserMode.SIMPLE_YAML_PARSER;
		} else {
			return ParserMode.PLAN_PARSER;
		}
	}

	private enum ParserMode {
		PLAN_PARSER,
		SIMPLE_YAML_PARSER
	}
}
