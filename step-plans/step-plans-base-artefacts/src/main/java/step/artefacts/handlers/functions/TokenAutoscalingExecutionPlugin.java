package step.artefacts.handlers.functions;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.functions.autoscaler.TokenAutoscalingConfiguration;
import step.artefacts.handlers.functions.autoscaler.DefaultTokenAutoscalingDriver;
import step.artefacts.handlers.functions.autoscaler.TokenAutoscalingDriver;
import step.artefacts.handlers.functions.autoscaler.TokenProvisioningRequest;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import java.util.Map;

/**
 * This plugin is responsible for the autoscaling of agent tokens.
 * It delegates the estimation of the required number of tokens (token forecasting)
 * and provisioning of the tokens to the configured driver
 */
@Plugin
public class TokenAutoscalingExecutionPlugin extends AbstractExecutionEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(TokenAutoscalingExecutionPlugin.class);
    public static final String CONTEXT_OBJECT_KEY = "$tokenForecastingContext";
    public static final String AUTOSCALER_DRIVER_PROPERTY = "grid.tokens.autoscaler.driver";
    private TokenAutoscalingDriver tokenAutoscalingDriver;

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
        super.initializeExecutionEngineContext(parentContext, executionEngineContext);
        // Get the autoscaling driver from the parent context or create it
        tokenAutoscalingDriver = executionEngineContext.inheritFromParentOrComputeIfAbsent(parentContext, TokenAutoscalingDriver.class, tokenAutoscalingDriverClass ->
                createAutoscalingDriver(executionEngineContext.getConfiguration()));
    }

    @Override
    public void provisionRequiredResources(ExecutionContext context) {
        super.provisionRequiredResources(context);

        // Get the results of the token forecasting
        TokenForecastingContext tokenForecastingContext = getTokenForecastingContext(context);
        Map<String, Integer> tokenForecastPerPool = tokenForecastingContext.getTokenForecastPerPool();
        logger.debug("Token forecast estimation result: " + tokenForecastPerPool);

        // Delegate the provisioning of the agent tokens to the driver according to the calculated forecast
        TokenProvisioningRequest request = new TokenProvisioningRequest();
        request.requiredNumberOfTokensPerPool = tokenForecastPerPool;
        tokenAutoscalingDriver.provisionTokens(request);
    }

    @Override
    public void executionStart(ExecutionContext context) {
        super.executionStart(context);

        TokenAutoscalingConfiguration autoscalerConfiguration = tokenAutoscalingDriver.getAutoscalerConfiguration();

        Map<String, Map<String, String>> pools = autoscalerConfiguration.availableTokenPools;
        TokenForecastingContext tokenForecastingContext = new TokenForecastingContext(pools);
        pushTokenForecastingContext(context, tokenForecastingContext, context.getReport());
    }

    private TokenAutoscalingDriver createAutoscalingDriver(Configuration configuration) {
        String driverClassname = configuration.getProperty(AUTOSCALER_DRIVER_PROPERTY);
        if (driverClassname != null) {
            try {
                Class<?> aClass = Class.forName(driverClassname);
                return (TokenAutoscalingDriver) aClass.getConstructor().newInstance();
            } catch (Exception e) {
                throw new PluginCriticalException("Error while creating token autoscaler driver", e);
            }
        } else {
            return new DefaultTokenAutoscalingDriver();
        }
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
