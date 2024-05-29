package step.cli;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AutomationPackageConsoleTest {

    @Test
    public void testWithoutConfigFile() throws Exception {
        AutomationPackageConsole console = new AutomationPackageConsole();

        console.setConfig(AutomationPackageConsole.DEFAULT_CONFIG_FILE);
        console.setCommand("deploy");

        URL fakeApUrl = Thread.currentThread().getContextClassLoader().getResource("step/cli/fakeAp.jar");
        Assert.assertNotNull(fakeApUrl);

        Path tempPath = Files.createTempFile(null, ".txt");

        try (InputStream is = fakeApUrl.openStream()) {
            console.setUrl("http://localhost:8080");

            Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
            console.setApFile(tempPath.toFile().getAbsolutePath());

            ApDeployCliHandlerMock handlerMock = getDeployHandlerMock();
            console.setApDeployHandler(handlerMock);

            console.call();

            Assert.assertTrue(handlerMock.invoked);
            // TODO: verify parameter values
        }
    }

    @Test
    public void testWithConfigFile() {
        // TODO: implement
    }

    @Test
    public void testWithMinimalConfig() {
        // TODO: implement
    }

    private ApDeployCliHandlerMock getDeployHandlerMock() {
        return new ApDeployCliHandlerMock();
    }

    private static class ApDeployCliHandlerMock extends ApDeployCliHandler {

        private Boolean invoked = false;
        private String stepUrl;
        private String artifactGroupId;
        private String artifactId;
        private String artifactVersion;
        private String artifactClassifier;
        private String stepProjectName;
        private String token;
        private Boolean async;
        private File apFile;

        @Override
        protected void runTool(String stepUrl, String artifactGroupId, String artifactId, String artifactVersion, String artifactClassifier, String stepProjectName, String token, Boolean async, File apFile) {
            invoked = true;
            this.stepUrl = stepUrl;
            this.artifactGroupId = artifactGroupId;
            this.artifactVersion = artifactVersion;
            this.artifactClassifier = artifactClassifier;
            this.stepProjectName = stepProjectName;
            this.token = token;
            this.async = async;
            this.apFile = apFile;
        }
    }
}