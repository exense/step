package step.cli;

import ch.exense.commons.io.FileHelper;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.AutomationPackageUpdateStatus;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.controller.multitenancy.Tenant;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;
import java.io.IOException;

public class DeployAutomationPackageToolTest {

    private static final ObjectId UPDATED_PACK_ID = new ObjectId();
    protected static final Tenant TENANT_1 = createTenant1();
    protected static final Tenant TENANT_2 = createTenant2();

    @Test
    public void testUpload() throws Exception {
        File testFile;
        try {
            testFile = FileHelper.createTempFile();
        } catch (IOException e) {
            throw new RuntimeException("Temp file cannot be created", e);
        }

        RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClientMock();
        DeployAutomationPackageToolTestable tool = new DeployAutomationPackageToolTestable(
                "http://localhost:8080", testFile, TENANT_1.getName(),
                null, false, "ver1", "true==true", null, null, automationPackageClient
        );
        tool.execute();

        // attributes used to search for existing function packages
        ArgumentCaptor<File> packageFileCaptor = ArgumentCaptor.forClass(File.class);
        Mockito.verify(automationPackageClient, Mockito.times(1))
                .createOrUpdateAutomationPackage(
                        packageFileCaptor.capture(), Mockito.isNull(), Mockito.anyBoolean(),
                        Mockito.eq("ver1"), Mockito.eq("true==true"),
                        Mockito.isNull(), Mockito.isNull()
                );
        Mockito.verify(automationPackageClient, Mockito.times(1)).close();
        Mockito.verifyNoMoreInteractions(automationPackageClient);
        Assert.assertEquals(testFile, packageFileCaptor.getValue());
    }

    private RemoteAutomationPackageClientImpl createRemoteAutomationPackageClientMock() throws AutomationPackageClientException {
        RemoteAutomationPackageClientImpl remoteClient = Mockito.mock(RemoteAutomationPackageClientImpl.class);
        Mockito.when(remoteClient.createOrUpdateAutomationPackage(
                Mockito.any(), Mockito.isNull(),
                Mockito.anyBoolean(),
                Mockito.any(), Mockito.any(),
                Mockito.isNull(), Mockito.isNull())
        ).thenReturn(new AutomationPackageUpdateResult(AutomationPackageUpdateStatus.CREATED, UPDATED_PACK_ID), null);
        return remoteClient;
    }

    private static class DeployAutomationPackageToolTestable extends DeployAutomationPackageTool {

        private RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock;

        public DeployAutomationPackageToolTestable(String url, File apFile, String stepProjectName, String authToken, Boolean async, String apVersion, String activationExpr,
                                                   MavenArtifactIdentifier keywordLibraryMavenIdentifier, File keywordLibraryFile,
                                                   RemoteAutomationPackageClientImpl remoteAutomationPackageClientMock) {
            super(url, new Params()
                    .setAutomationPackageFile(apFile)
                    .setStepProjectName(stepProjectName)
                    .setAuthToken(authToken)
                    .setAsync(async)
                    .setApVersion(apVersion)
                    .setActivationExpression(activationExpr)
                    .setKeywordLibraryMavenArtifact(keywordLibraryMavenIdentifier)
                    .setKeywordLibraryFile(keywordLibraryFile));
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