package step.ide.api;

import step.agents.provisioning.local.LocalAgentProvisioningConfiguration;
import step.automation.packages.AutomationPackageUpdateResult;

import java.nio.file.Path;
import java.util.List;

public interface IDEDelegator {

    LocalExecutionDelegate delegateLocalExecution(LocalExecutionRequest request);

    /**
     * Executes the automation package and returns the executions this started, one per plan unless the plans are
     * wrapped into a single test set.
     */
    List<RemoteExecution> executeOnStep(Path apPath, RemoteExecutionRequest request) throws Exception;

    AutomationPackageUpdateResult deploy(Path apPath, RemoteDeploymentRequest request) throws Exception;

    /**
     * Returns the options that remote executions and deployments fall back to, as configured in the CLI properties.
     */
    RemoteDefaults remoteDefaults();

    /**
     * Returns the local agent provisioning options configured in the CLI properties.
     */
    LocalAgentProvisioningConfiguration localAgentConfiguration();
}
