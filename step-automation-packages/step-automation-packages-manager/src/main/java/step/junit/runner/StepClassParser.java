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
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.handlers.javahandler.Keyword;
import step.junit.runners.annotations.PlanCategories;
import step.junit.runners.annotations.Plans;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;
import step.plans.parser.yaml.YamlPlanReader;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class StepClassParser {
	private static final Logger logger = LoggerFactory.getLogger(StepClassParser.class);

	private final boolean appendClassnameToPlanName;

	private final PlanParser planParser = new PlanParser();

	private YamlPlanReader simpleYamlPlanReader;

	public StepClassParser(boolean appendClassnameToPlanName) {
		super();
		this.appendClassnameToPlanName = appendClassnameToPlanName;
		this.simpleYamlPlanReader = null;
	}


	public List<StepClassParserResult> getPlanFromAnnotatedMethods(AnnotationScanner annotationScanner) {
		return annotationScanner.getMethodsWithAnnotation(step.junit.runners.annotations.Plan.class).stream()
				.map(m -> {
					Keyword keyword = m.getAnnotation(Keyword.class);
					String planName;
					if (keyword != null && keyword.name() != null && !keyword.name().isEmpty()) {
						planName = keyword.name();
					} else {
						planName = (appendClassnameToPlanName ? m.getDeclaringClass().getName() + "." : "") + m.getName();
					}

					Exception exception = null;
					Plan plan = null;
					try {
						String planStr = m.getAnnotation(step.junit.runners.annotations.Plan.class).value();
						if (planStr.trim().length() == 0) {
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
                        YamlPlanReader.setPlanName(plan, planName);
						PlanCategories planCategories = m.getAnnotation(PlanCategories.class);
						if (planCategories != null && planCategories.value() != null) {
							plan.setCategories(Arrays.asList(planCategories.value()));
						}
					} catch (Exception e) {
						exception = e;
					}
					return new StepClassParserResult(planName, plan, exception);
				}).collect(Collectors.toList());
	}


	public StepClassParserResult createPlan(Class<?> klass, String fileName, InputStream stream) {
		Plan plan = null;
		Exception exception = null;
		try {
			ParserMode parserMode = chooseParserModeByFileName(fileName);

			if (stream == null) {
				throw new Exception("Plan '" + fileName + "' was not found for class " + klass.getName());
			}

			if (parserMode == ParserMode.PLAIN_TEXT_PARSER) {
				plan = planParser.parse(stream, RootArtefactType.TestCase);
				logger.debug("plan is:" + plan + " for class " + klass.getName());
				YamlPlanReader.setPlanName(plan, fileName);
			} else if (parserMode == ParserMode.YAML_PARSER) {
				try {
					plan = initAndGetYamlPlanReader().readYamlPlan(stream);
				} catch (Exception e) {
					// wrap into another exception to include the fileName to error details
					throw new RuntimeException(fileName + " processing error", e);
				}
			} else {
				throw new UnsupportedOperationException("Unable to resolve plan parser " + parserMode);
			}
			PlanCategories planCategories = klass.getAnnotation(PlanCategories.class);
			if (planCategories != null && planCategories.value() != null) {
				plan.setCategories(Arrays.asList(planCategories.value()));
			}
		} catch (Exception e) {
			exception = e;
		}

		return new StepClassParserResult(fileName, plan, exception);
	}

	private ParserMode chooseParserModeByFileName(String fileName) {
		if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
			return ParserMode.YAML_PARSER;
		} else {
			return ParserMode.PLAIN_TEXT_PARSER;
		}
	}

	private synchronized YamlPlanReader initAndGetYamlPlanReader() {
		if (simpleYamlPlanReader == null) {
			this.simpleYamlPlanReader = new YamlPlanReader();
		}
		return this.simpleYamlPlanReader;
	}

	private enum ParserMode {
		PLAIN_TEXT_PARSER,
		YAML_PARSER
	}
}
