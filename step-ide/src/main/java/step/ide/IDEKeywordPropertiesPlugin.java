package step.ide;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;
import step.core.variables.VariablesManager;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.expressions.ProtectedVariable;

import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Injects the IDE properties prefixed with {@value #KEYWORD_PROPERTY_PREFIX} into every execution, so that they reach
 * the keywords as input properties.
 * <p>
 * This is the channel through which secrets such as the Anthropic API key of the AI agent are provided. It exists
 * because neither of the usual mechanisms works in the IDE: automation package parameters would be written into the
 * user's <code>parameters.yml</code> and committed, and controller level parameters are unavailable
 * (the parameters collection is backed by the opened package, and the parameter and encryption plugins are disabled
 * in ide.properties).
 * <p>
 * Values are published the same way {@link step.engine.plugins.BasePlugin} publishes the execution custom parameters:
 * as immutable variables on the root report node, which
 * {@link step.artefacts.handlers.CallFunctionHandler} then copies into the keyword input properties. Secret looking
 * keys are wrapped in a {@link ProtectedVariable} so they are obfuscated in the UI and in logs while the keyword
 * still receives the real value.
 */
@Plugin
@IgnoreDuringAutoDiscovery
public class IDEKeywordPropertiesPlugin extends AbstractExecutionEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(IDEKeywordPropertiesPlugin.class);

    public static final String KEYWORD_PROPERTY_PREFIX = "keyword.property.";
    public static final String ENV_ANTHROPIC_API_KEY = "ANTHROPIC_API_KEY";

    private static final Pattern SECRET_KEY_PATTERN = Pattern.compile(".*(api[_-]?key|token|secret|password).*", Pattern.CASE_INSENSITIVE);

    private Configuration configuration;

    protected IDEKeywordPropertiesPlugin() {
    }

    public IDEKeywordPropertiesPlugin(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void executionStart(ExecutionContext context) {
        super.executionStart(context);
        if (configuration == null) {
            return;
        }
        Properties properties = configuration.getUnderlyingPropertyObject();
        ReportNode rootNode = context.getReport();
        VariablesManager variablesManager = context.getVariablesManager();

        // TODO instead of doing this in a generic way, we should add the properties required by the AI agent to the IDEAiConfiguration, retrieve the IDEAiConfiguration object here and inject them here
        properties.stringPropertyNames().stream()
            .filter(key -> key.startsWith(KEYWORD_PROPERTY_PREFIX))
            .forEach(key -> {
                String value = properties.getProperty(key);
                if (value == null || value.isBlank()) {
                    return;
                }
                String name = key.substring(KEYWORD_PROPERTY_PREFIX.length());
                boolean secret = SECRET_KEY_PATTERN.matcher(name).matches();
                logger.debug("Injecting IDE keyword property {}{}", name, secret ? " (protected)" : "");
                Object variableValue = secret ? new ProtectedVariable(name, value) : value;
                variablesManager.putVariable(rootNode, VariableType.IMMUTABLE, name, variableValue);
            });
    }
}
