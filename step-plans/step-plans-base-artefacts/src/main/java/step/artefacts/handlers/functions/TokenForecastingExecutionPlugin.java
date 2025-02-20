/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.artefacts.handlers.functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.agents.provisioning.AgentPoolSpec;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.threadpool.ThreadPool;

import java.util.Set;

/**
 * This plugin is responsible for the token forecasting.
 * It delegates the estimation of the required number of tokens (token forecasting)
 * and provisioning of the tokens to the configured driver
 */
@Plugin
public class TokenForecastingExecutionPlugin extends AbstractExecutionEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(TokenForecastingExecutionPlugin.class);
    public static final String CONTEXT_OBJECT_KEY = "$tokenForecastingContext";

    private Set<AgentPoolSpec> availableAgentPools;

    public TokenForecastingExecutionPlugin() {
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        if (availableAgentPools == null) {
            AgentProvisioningDriver agentProvisioningDriver = executionEngineContext.get(AgentProvisioningDriver.class);
            availableAgentPools = (agentProvisioningDriver != null) ?
                    agentProvisioningDriver.getConfiguration().availableAgentPools :
                    Set.of();
        }
    }

    @Override
    public void executionStart(ExecutionContext context) {
        super.executionStart(context);

        // Token forecasting is always calculated, even if the agent provisioning is disabled
        TokenForecastingContext tokenForecastingContext = new TokenForecastingContext(availableAgentPools);
        String executionThreadsAuto = context.getVariablesManager().getVariableAsString(ThreadPool.EXECUTION_THREADS_AUTO, null);
        if (executionThreadsAuto != null && !executionThreadsAuto.isEmpty()) {
            try {
                tokenForecastingContext.setMaxParallelism(Integer.parseInt(executionThreadsAuto));
            } catch (NumberFormatException e) {
                logger.warn("Ignoring unparseable value for parameter {}: {}", ThreadPool.EXECUTION_THREADS_AUTO, executionThreadsAuto, e);
            }
        }
        pushTokenForecastingContext(context, tokenForecastingContext, context.getReport());
    }

    public static TokenForecastingContext getTokenForecastingContext(ExecutionContext executionContext) {
        return (TokenForecastingContext) executionContext.getVariablesManager().getVariable(CONTEXT_OBJECT_KEY);
    }

    public static void pushNewTokenNumberCalculationContext(ExecutionContext executionContext, TokenForecastingContext newTokenForecastingContext) {
        ReportNode reportNode = executionContext.getCurrentReportNode();
        pushTokenForecastingContext(executionContext, newTokenForecastingContext, reportNode);
    }

    private static void pushTokenForecastingContext(ExecutionContext executionContext, TokenForecastingContext newTokenForecastingContext, ReportNode reportNode) {
        executionContext.getVariablesManager().putVariable(reportNode, CONTEXT_OBJECT_KEY, newTokenForecastingContext);
    }
}
