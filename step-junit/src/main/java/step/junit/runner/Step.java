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

import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.junit.runners.annotations.ExecutionParameters;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Step extends ParentRunner<StepClassParserResult> {

	private static final Logger logger = LoggerFactory.getLogger(Step.class);

	private final Class<?> klass;
	private final List<StepClassParserResult> listPlans;

	private final ExecutionEngine executionEngine;
	private ResourceManager resourceManager;

	public Step(Class<?> klass) throws InitializationError {
		super(klass);

		this.klass = klass;
		try {
			executionEngine = ExecutionEngine.builder().withPlugin(new AbstractExecutionEnginePlugin() {
				@Override
				public void afterExecutionEnd(ExecutionContext context) {
					resourceManager = context.getResourceManager();
				}
			}).withPluginsFromClasspath().build();
			StepClassParser classParser = new StepClassParser(false);
			listPlans = classParser.createPlansForClass(klass);
		} catch (Exception e) {
			throw new InitializationError(e);
		}
	}

	@Override
	protected Description describeChild(StepClassParserResult child) {
		return Description.createTestDescription(klass, child.getName());
	}

	@Override
	protected void runChild(StepClassParserResult child, RunNotifier notifier) {
		Description desc = Description.createTestDescription(klass, child.getName());
		EachTestNotifier childNotifier = new EachTestNotifier(notifier, desc);

		childNotifier.fireTestStarted();

		try {
			Exception initializingException = child.getInitializingException();
			if (initializingException == null) {
				Plan plan = child.getPlan();
				Map<String, String> executionParameters = getExecutionParameters();
				PlanRunnerResult result = executionEngine.execute(plan, executionParameters);
				ReportNodeStatus resultStatus = result.getResult();

				if (resultStatus == ReportNodeStatus.PASSED) {
					// We actually also want to see results when tests complete successfully
					result.printTree();
				} else if (resultStatus == ReportNodeStatus.FAILED) {
					notifyFailure(childNotifier, result, "Plan execution failed", true);
				} else if (resultStatus == ReportNodeStatus.TECHNICAL_ERROR) {
					notifyFailure(childNotifier, result, "Technical error while executing plan", false);
				} else {
					notifyFailure(childNotifier, result, "The plan execution returned an unexpected status\" + result",
							false);
				}
			} else {
				childNotifier.addFailure(initializingException);
			}
		} catch (Exception e) {
			childNotifier.addFailure(e);
		} finally {
			if (resourceManager instanceof LocalResourceManagerImpl) {
				// Cleanup resource manager after execution
				((LocalResourceManagerImpl) resourceManager).cleanup();
			}
			childNotifier.fireTestFinished();
		}
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

	private Map<String, String> getExecutionParametersFromSystemProperties() {
		Map<String, String> executionParameters = new HashMap<>();
		System.getProperties().forEach((k, v) -> executionParameters.put(k.toString(), v.toString()));
		return executionParameters;
	}

	private final static Pattern ENV_PARAM_PREFIX = Pattern.compile("STEP_(.+?)");

	private Map<String, String> getExecutionParametersFromEnvironmentVariables() {
		Map<String, String> executionParameters = new HashMap<>();
		System.getenv().forEach((k, v) -> {
			Matcher matcher = ENV_PARAM_PREFIX.matcher(k);
			if (matcher.matches()) {
				String key = matcher.group(1);
				executionParameters.put(key, v);
			}
		});
		return executionParameters;
	}

	protected void notifyFailure(EachTestNotifier childNotifier, PlanRunnerResult res, String errorMsg,
			boolean assertionError) {
		String executionTree = getExecutionTreeAsString(res);
		String detailMessage = errorMsg + "\nExecution tree is:\n" + executionTree;
		if (assertionError) {
			childNotifier.addFailure(new AssertionError(detailMessage));
		} else {
			childNotifier.addFailure(new Exception(detailMessage));
		}
	}

	private static String getExecutionTreeAsString(PlanRunnerResult res) {
		String executionTree;
		Writer w = new StringWriter();
		try {
			res.printTree(w, true, true);
			executionTree = w.toString();
		} catch (IOException e) {
			logger.error("Error while writing execution tree", w);
			executionTree = "Error while writing tree. See logs for details.";
		}
		return executionTree;
	}

	@Override
	protected List<StepClassParserResult> getChildren() {
		return listPlans;
	}
}
