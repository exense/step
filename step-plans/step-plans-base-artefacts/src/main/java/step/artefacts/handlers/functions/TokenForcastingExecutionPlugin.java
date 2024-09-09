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

import java.util.Set;

/**
 * This plugin is responsible for the autoscaling of agent tokens.
 * It delegates the estimation of the required number of tokens (token forecasting)
 * and provisioning of the tokens to the configured driver
 */
@Plugin
public class TokenForcastingExecutionPlugin extends AbstractExecutionEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(TokenForcastingExecutionPlugin.class);
    public static final String CONTEXT_OBJECT_KEY = "$tokenForecastingContext";

    private Set<AgentPoolSpec> pools;

    public TokenForcastingExecutionPlugin() {
    }

    @Override
    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
        if (pools == null) {
            AgentProvisioningDriver agentProvisioningDriver = executionEngineContext.get(AgentProvisioningDriver.class);
            pools = (agentProvisioningDriver != null) ?
                    agentProvisioningDriver.getConfiguration().availableAgentPools :
                    Set.of();
        }
    }

    @Override
    public void executionStart(ExecutionContext context) {
        super.executionStart(context);

        // Token forecasting is always calculated, even if the autoscaling is disabled
        TokenForecastingContext tokenForecastingContext = new TokenForecastingContext(pools);
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
