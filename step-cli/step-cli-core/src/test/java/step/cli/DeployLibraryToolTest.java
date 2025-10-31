package step.cli;

import ch.exense.commons.io.FileHelper;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.AutomationPackageUpdateStatus;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.automation.packages.client.model.AutomationPackageSource;
import step.cli.parameters.LibraryDeployParameters;
import step.controller.multitenancy.Tenant;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class DeployLibraryToolTest {

    private static final ObjectId UPDATED_LIB_ID = new ObjectId();
    protected static final Tenant TENANT_1 = createTenant1();

    @Test
    public void testDeployLibrary() throws Exception {
        File testFile;
        try {
            testFile = FileHelper.createTempFile();
        } catch (IOException e) {
            throw new RuntimeException("Temp file cannot be created", e);
        }

        RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClientMock();
        DeployLibraryToolTestable tool = new DeployLibraryToolTestable(
                "http://localhost:8080", TENANT_1.getName(),
                null, null, testFile, null, automationPackageClient
        );
        tool.execute();

        // attributes used to search for existing function packages
        ArgumentCaptor<AutomationPackageSource> packageFileCaptor = ArgumentCaptor.forClass(AutomationPackageSource.class);
        Mockito.verify(automationPackageClient, Mockito.times(1))
                .createOrUpdateAutomationPackageLibrary(
                        packageFileCaptor.capture(), Mockito.isNull()

                );
        Mockito.verify(automationPackageClient, Mockito.times(1)).close();
        Mockito.verifyNoMoreInteractions(automationPackageClient);
        Assert.assertEquals(testFile, packageFileCaptor.getValue().getFile());
    }

    private RemoteAutomationPackageClientImpl createRemoteAutomationPackageClientMock() throws AutomationPackageClientException {
        RemoteAutomationPackageClientImpl remoteClient = Mockito.mock(RemoteAutomationPackageClientImpl.class);
        Mockito.when(remoteClient.createOrUpdateAutomationPackageLibrary(
                Mockito.any(), Mockito.any())
        ).thenReturn(new AutomationPackageUpdateResult(AutomationPackageUpdateStatus.CREATED, UPDATED_LIB_ID, null, Set.of()));
        return remoteClient;
    }

    private static class DeployLibraryToolTestable extends DeployLibraryTool {

        private final RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock;

        public DeployLibraryToolTestable(String url, String stepProjectName, String authToken,
                                                   MavenArtifactIdentifier mavenIdentifier, File file, String managedLibraryName,
                                                   RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock) {
            super(url, new LibraryDeployParameters()
                    .setStepProjectName(stepProjectName)
                    .setAuthToken(authToken)
                    .setPackageLibraryMavenArtifact(mavenIdentifier)
                    .setPackageLibraryFile(file)
                    .setManagedLibraryName(managedLibraryName));
            this.remoteAutomationPackageClientMock = remoteAutomationPackageClientMock;
        }

        @Override
        protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
            return remoteAutomationPackageClientMock;
        }

    }

    protected static Tenant createTenant1() {
        Tenant tenant1 = new Tenant();
        tenant1.setName("project1");
        tenant1.setProjectId(new ObjectId().toString());
        return tenant1;
    }

    protected static Tenant createTenant2() {
        Tenant tenant2 = new Tenant();
        tenant2.setName("project2");
        tenant2.setProjectId(new ObjectId().toString());
        return tenant2;
    }

}