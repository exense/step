package step.cli;

import org.junit.Test;
import step.agents.provisioning.local.LocalAgentProvisioningConfiguration;
import step.ide.api.RemoteDefaults;
import step.ide.api.RemoteDeploymentRequest;
import step.ide.api.RemoteExecutionRequest;
import step.ide.api.StepConnectionInfo;
import step.ide.exceptions.InvalidRequestException;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class IdeRemoteDelegateTest {

    private static final Path AP_DIRECTORY = Path.of("some", "automation-package");

    private static IdeRemoteDelegate delegateWith(String... configFiles) {
        return new IdeRemoteDelegate(new StepDefaultValuesProvider(List.of(configFiles), false));
    }

    private static String resource(String name) {
        return Path.of("src", "test", "resources", name).toAbsolutePath().toString();
    }

    @Test
    public void defaultsAreReadFromTheConfigurationFiles() {
        RemoteDefaults defaults = delegateWith(resource("customCli.properties"), resource("customCli2.properties"))
            .remoteDefaults();

        StepConnectionInfo connection = defaults.deploy().connection();
        assertEquals("http://localhost:8081", connection.url());
        assertEquals("testProject", connection.projectName());
        assertEquals(Map.of("key2", "defaultValue2", "key3", "defaultValue3", "key4", "prioDefaultValue4"),
            defaults.execute().executionParameters());
    }

    @Test
    public void theTokenIsReportedAsConfiguredButNotDisclosed() {
        RemoteDefaults configured = delegateWith(resource("customCli.properties"), resource("customCli2.properties"))
            .remoteDefaults();
        assertNull(configured.deploy().connection().token());
        assertTrue(configured.deploy().connection().tokenConfigured());
        assertTrue(configured.execute().connection().tokenConfigured());

        RemoteDefaults withoutToken = delegateWith(resource("customCli.properties")).remoteDefaults();
        assertNull(withoutToken.deploy().connection().token());
        assertFalse(withoutToken.deploy().connection().tokenConfigured());
    }

    @Test
    public void deploymentOptionsAreReadFromTheConfigurationFile() {
        RemoteDeploymentRequest deploy = delegateWith(resource("customCliDeploymentConfigurations.properties"))
            .remoteDefaults().deploy();

        assertEquals(Map.of("planKey1", "defaultPlanValue1", "planKey2", "defaultPlanValue2"), deploy.plansAttributes());
        assertEquals(Map.of("keywordKey1", "defaultKeywordValue1"), deploy.keywordsAttributes());
        assertEquals(Map.of("os", "linux"), deploy.tokenSelectionCriteria());
        assertEquals(Boolean.TRUE, deploy.executeKeywordsOnController());
        // Not configured, so the default of the CLI option applies
        assertEquals(Integer.valueOf(300), deploy.deploymentTimeout());
        assertEquals(Boolean.FALSE, deploy.async());
    }

    @Test
    public void aDeploymentRequestOverridesOnlyTheOptionsItSets() {
        IdeRemoteDelegate delegate = delegateWith(resource("customCli.properties"), resource("customCli2.properties"));
        RemoteDeploymentRequest request = new RemoteDeploymentRequest(
            new StepConnectionInfo(null, "otherProject", null, null),
            null, null, "v2", null, null, null, null, null, null, null);

        ApCommand.ApDeployCommand command = delegate.prepareDeploy(AP_DIRECTORY, request);

        assertEquals("otherProject", command.stepProjectName);
        assertEquals("v2", command.versionName);
        // Left untouched by the request, so still coming from the configuration files
        assertEquals("http://localhost:8081", command.stepUrl);
        assertEquals("abc", command.authToken);
        assertEquals(AP_DIRECTORY.toAbsolutePath().toString(), command.apFile);
    }

    @Test
    public void anExecutionRequestOverridesOnlyTheOptionsItSets() {
        IdeRemoteDelegate delegate = delegateWith(resource("customCli.properties"), resource("customCli2.properties"));
        RemoteExecutionRequest request = new RemoteExecutionRequest(
            null, null, List.of("PlanC"), null, null, null, null, null, Map.of("env", "PROD"));

        ApCommand.ApExecuteCommand command = delegate.prepareExecute(AP_DIRECTORY, request);

        assertEquals(List.of("PlanC"), command.includePlans);
        assertEquals(Map.of("env", "PROD"), command.executionParameters);
        assertEquals("http://localhost:8081", command.stepUrl);
        assertEquals("testProject", command.stepProjectName);
        assertEquals("abc", command.authToken);
        // The IDE always starts the execution without waiting for it, and never runs it locally
        assertTrue(command.async);
        assertFalse(command.local);
    }

    @Test
    public void localAgentOptionsAreReadFromTheConfigurationFile() {
        LocalAgentProvisioningConfiguration configuration =
            delegateWith(resource("customCliLocalAgent.properties")).localAgentConfiguration();

        assertEquals(Path.of("/opt/step/java-agent"), configuration.getJavaAgentPath());
        assertEquals(12, configuration.getMaxTokensPerAgent());
        assertEquals(List.of("-Xms1g"), configuration.getJavaAgentVmArgs());
        // Not configured, so the defaults of the CLI options apply
        assertNull(configuration.getNodeAgentPath());
        assertEquals(Duration.ofSeconds(LocalAgentProvisioningConfiguration.DEFAULT_START_TIMEOUT_SECONDS),
            configuration.getAgentStartTimeout());
    }

    @Test
    public void localAgentOptionsFallBackToTheirDefaultsWithoutConfiguration() {
        LocalAgentProvisioningConfiguration configuration = delegateWith().localAgentConfiguration();

        assertNull(configuration.getJavaAgentPath());
        assertNull(configuration.getWorkDirectory());
        assertEquals(LocalAgentProvisioningConfiguration.DEFAULT_MAX_TOKENS_PER_AGENT, configuration.getMaxTokensPerAgent());
        assertEquals(List.of(), configuration.getJavaAgentVmArgs());
    }

    @Test
    public void anIncompleteConnectionIsReportedAsAnInvalidRequest() {
        IdeRemoteDelegate delegate = delegateWith();

        InvalidRequestException missingUrl = assertThrows(InvalidRequestException.class,
            () -> delegate.deploy(AP_DIRECTORY, RemoteDeploymentRequest.DEFAULTS));
        assertTrue(missingUrl.getMessage(), missingUrl.getMessage().contains(StepConsole.AbstractStepCommand.STEP_URL));

        RemoteDeploymentRequest tokenWithoutProject = new RemoteDeploymentRequest(
            new StepConnectionInfo("http://localhost:8080", null, "abc", null),
            null, null, null, null, null, null, null, null, null, null);
        InvalidRequestException missingProject = assertThrows(InvalidRequestException.class,
            () -> delegate.deploy(AP_DIRECTORY, tokenWithoutProject));
        assertTrue(missingProject.getMessage(),
            missingProject.getMessage().contains(StepConsole.AbstractStepCommand.PROJECT_NAME));
    }
}
