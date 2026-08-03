package step.ide.ai;

import ch.exense.commons.app.Configuration;

import java.io.File;

/**
 * Configuration of the AI assisted test case creation, read from ide.properties and/or environment variables.
 * <p>
 * The AI agent is delivered as a <b>packaged</b> automation package: {@link #agentPackage()} points at that archive,
 * and {@link #agentPlanName()} selects the plan implementing the agentic workflow. Restricting the execution to that
 * single plan is mandatory, as an isolated automation package execution otherwise runs every plan of the package.
 */
public record IDEAiConfiguration(boolean enabled, File agentPackage, String agentPlanName, String workflow) {

    public static final String PROP_ENABLED = "plugins.ide.ai.enabled";
    public static final String PROP_AGENT_PACKAGE = "plugins.ide.ai.agentPackageFile";
    public static final String PROP_AGENT_PLAN = "plugins.ide.ai.agentPlanName";
    public static final String PROP_WORKFLOW = "plugins.ide.ai.workflow";

    public static final String ENV_AGENT_PACKAGE = "STEP_IDE_AI_AGENT_PACKAGE";
    public static final String ENV_AGENT_PLAN = "STEP_IDE_AI_AGENT_PLAN";

    public static final String DEFAULT_WORKFLOW = "test-generation";

    public static IDEAiConfiguration from(Configuration configuration) {
        boolean enabled = configuration.getPropertyAsBoolean(PROP_ENABLED, true);
        String agentPackagePath = trimToNull(configuration.getProperty(PROP_AGENT_PACKAGE));
        return new IDEAiConfiguration(
            enabled,
            agentPackagePath == null ? null : new File(agentPackagePath),
            trimToNull(configuration.getProperty(PROP_AGENT_PLAN)),
            configuration.getProperty(PROP_WORKFLOW, DEFAULT_WORKFLOW)
        );
    }

    /**
     * @return null when the feature is usable, otherwise a message explaining to the user how to make it usable
     */
    public String unavailabilityReason() {
        if (!enabled) {
            return "AI assisted test case creation is disabled (" + PROP_ENABLED + "=false).";
        }
        if (agentPackage == null) {
            return "AI assisted test case creation is not configured. Set the environment variable " + ENV_AGENT_PACKAGE
                + " (or the property " + PROP_AGENT_PACKAGE + " in ide.properties) to the packaged automation package of the AI agent.";
        }
        if (!agentPackage.isFile() || !agentPackage.canRead()) {
            return "The configured AI agent automation package is not a readable file: " + agentPackage.getAbsolutePath();
        }
        if (agentPlanName == null) {
            return "The plan implementing the agentic workflow is not configured. Set the environment variable " + ENV_AGENT_PLAN
                + " (or the property " + PROP_AGENT_PLAN + " in ide.properties).";
        }
        return null;
    }

    public boolean isAvailable() {
        return unavailabilityReason() == null;
    }

    public void validate() {
        String reason = unavailabilityReason();
        if (reason != null) {
            throw new IllegalStateException(reason);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
