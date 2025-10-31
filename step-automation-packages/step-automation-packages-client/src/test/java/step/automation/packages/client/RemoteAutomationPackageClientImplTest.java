package step.automation.packages.client;

import org.junit.Ignore;
import org.junit.Test;
import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.client.model.AutomationPackageSource;
import step.client.credentials.ControllerCredentials;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipOutputStream;

public class RemoteAutomationPackageClientImplTest {

    @Test
    @Ignore
    public void testUploadIntegration() throws Exception {
        File testFile;
        try {
            testFile = Files.createTempFile(null, ".zip").toFile();
            testFile.deleteOnExit();
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(testFile))) {
                // Simply close the stream without adding any entries
                // This creates a valid empty ZIP file
            }
        } catch (IOException e) {
            throw new RuntimeException("Temp file cannot be created", e);
        }

        AutomationPackageSource fileSource = AutomationPackageSource.withFile(testFile);
        try (RemoteAutomationPackageClientImpl automationPackageClient = new RemoteAutomationPackageClientImpl(new ControllerCredentials("http://localhost:8080", "admin", "init"))) {
            AutomationPackageUpdateResult ver1 = automationPackageClient.createOrUpdateAutomationPackage(fileSource, null, "v1", "env == DEV",
                    null, Map.of("FunctionAttr1", "FunctionAttr1Value"), Map.of("OS", "LINUX"), null, true, false);
        }


    }
}
