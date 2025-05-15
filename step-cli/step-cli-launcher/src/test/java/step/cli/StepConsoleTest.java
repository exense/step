package step.cli;

import ch.exense.commons.io.FileHelper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.client.controller.ControllerServicesClient;
import step.core.Constants;
import step.core.Version;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StepConsoleTest {

    private static final Logger log = LoggerFactory.getLogger(StepConsoleTest.class);

    @Test
    public void testHelp() {
        List<TestApDeployCommand.ExecutionParams> deployExecRegistry = new ArrayList<>();
        List<TestApExecuteCommand.RemoteExecutionParams> remoteExecutionParams = new ArrayList<>();
        List<TestApExecuteCommand.LocalExecutionParams> localExecutionParams = new ArrayList<>();
        Histories histories = new Histories(deployExecRegistry, remoteExecutionParams, localExecutionParams);

        int res = runMain(histories, "help");
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());
        Assert.assertEquals(0, remoteExecutionParams.size());
        Assert.assertEquals(0, localExecutionParams.size());

        res = runMain(histories, "ap", "help");
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());
        Assert.assertEquals(0, remoteExecutionParams.size());
        Assert.assertEquals(0, localExecutionParams.size());

        res = runMain(histories, "ap", "deploy", "help");
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());
        Assert.assertEquals(0, remoteExecutionParams.size());
        Assert.assertEquals(0, localExecutionParams.size());

        res = runMain(histories, "ap", "execute", "help");
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());
        Assert.assertEquals(0, remoteExecutionParams.size());
        Assert.assertEquals(0, localExecutionParams.size());

        // help is called by default for intermediate subcommands
        res = runMain(histories);
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());
        Assert.assertEquals(0, remoteExecutionParams.size());
        Assert.assertEquals(0, localExecutionParams.size());

        res = runMain(histories, "ap");
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());
        Assert.assertEquals(0, remoteExecutionParams.size());
        Assert.assertEquals(0, localExecutionParams.size());
    }

    @Test
    public void testDeployAp() {
        List<TestApDeployCommand.ExecutionParams> deployExecHistory = new ArrayList<>();

        Histories histories = new Histories(deployExecHistory, null, null);

        // for EE
        int res = runMain(histories, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080", "--projectName=testProject", "--token=abc", "--async", "--apVersion=ver1", "--activationExpr=true==true");

        Assert.assertEquals(0, res);
        Assert.assertEquals(1, deployExecHistory.size());
        TestApDeployCommand.ExecutionParams usedParams = deployExecHistory.get(0);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertEquals("abc", usedParams.authToken);
        Assert.assertEquals("testProject", usedParams.projectName);
        Assert.assertTrue(usedParams.async);
        Assert.assertEquals("step-automation-packages-sample1.jar", new File(usedParams.apFile).getName());
        Assert.assertEquals(usedParams.apVersion, "ver1");
        Assert.assertEquals(usedParams.activationExpr, "true==true");

        // for OS (required params only)
        deployExecHistory.clear();
        res = runMain(histories, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, deployExecHistory.size());
        usedParams = deployExecHistory.get(0);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertFalse(usedParams.async);
        Assert.assertEquals("step-automation-packages-sample1.jar", new File(usedParams.apFile).getName());

        // incorrect parameters (project name / token)
        deployExecHistory.clear();
        res = runMain(histories, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080", "--async", "--projectName=testProject");
        Assert.assertEquals(2, res);

        res = runMain(histories, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080", "--async", "--token=abc");
        Assert.assertEquals(2, res);

        // properties file (with url http://localhost:8081)
        deployExecHistory.clear();
        res = runMain(histories, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-c=src/test/resources/customCli.properties");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, deployExecHistory.size());
        usedParams = deployExecHistory.get(0);
        Assert.assertEquals("http://localhost:8081", usedParams.stepUrl);
        Assert.assertEquals("step-automation-packages-sample1.jar", new File(usedParams.apFile).getName());

        // several properties files (containing url and project/token)
        deployExecHistory.clear();
        res = runMain(histories, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-c=src/test/resources/customCli.properties", "-c=src/test/resources/customCli2.properties");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, deployExecHistory.size());
        usedParams = deployExecHistory.get(0);
        Assert.assertEquals("http://localhost:8081", usedParams.stepUrl);
        Assert.assertEquals("step-automation-packages-sample1.jar", new File(usedParams.apFile).getName());
        Assert.assertEquals("abc", usedParams.authToken);
        Assert.assertEquals("testProject", usedParams.projectName);

        // deploy from artifactory
        deployExecHistory.clear();
        res = runMain(histories, "ap", "deploy", "-p=mvn:ch.exense.step:step-automation-packages-junit:0.0.0:tests", "-u=http://localhost:8080",  "--apVersion=1.0.0");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, deployExecHistory.size());
        usedParams = deployExecHistory.get(0);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertEquals("ch.exense.step", usedParams.mavenArtifact.getGroupId());
        Assert.assertEquals("step-automation-packages-junit", usedParams.mavenArtifact.getArtifactId());
        Assert.assertEquals("0.0.0", usedParams.mavenArtifact.getVersion());
        Assert.assertEquals("tests", usedParams.mavenArtifact.getClassifier());
    }

    @Test
    public void testRemoteExecute() {
        List<TestApExecuteCommand.RemoteExecutionParams> remoteExecuteHistory = new ArrayList<>();

        Histories histories = new Histories(null, remoteExecuteHistory, null);

        // all parameters
        int res = runMain(histories, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar",
                "-u=http://localhost:8080", "--projectName=testProject", "--token=abc", "--async", "--includePlans=p1,p2",
                "--includeCategories=CatA,CatB", "--excludeCategories=CatC,CatD",
                "--executionTimeoutS=1000",
                "--excludePlans=p3,p4", "-ep=key1=value1|key2=value2", "-ep=key3=value3"
        );

        Assert.assertEquals(0, res);
        Assert.assertEquals(1, remoteExecuteHistory.size());
        TestApExecuteCommand.RemoteExecutionParams usedParams = remoteExecuteHistory.get(0);
        Assert.assertEquals("p1,p2", usedParams.params.getIncludePlans());
        Assert.assertEquals("p3,p4", usedParams.params.getExcludePlans());
        Assert.assertEquals("CatA,CatB", usedParams.params.getIncludeCategories());
        Assert.assertEquals("CatC,CatD", usedParams.params.getExcludeCategories());
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertEquals("abc", usedParams.params.getAuthToken());
        Assert.assertEquals("testProject", usedParams.params.getStepProjectName());
        Assert.assertEquals("timeout doesn't match", (Integer) 1000, usedParams.params.getExecutionResultTimeoutS());
        Assert.assertFalse(usedParams.params.getWaitForExecution());
        Assert.assertEquals(Map.of("key1", "value1", "key2", "value2", "key3", "value3"), usedParams.params.getExecutionParameters());
        Assert.assertEquals("step-automation-packages-sample1.jar", new File(usedParams.apFile).getName());
        Assert.assertNull(usedParams.params.getMavenArtifactIdentifier());

        // minimum parameters
        remoteExecuteHistory.clear();
        res = runMain(histories, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, remoteExecuteHistory.size());
        usedParams = remoteExecuteHistory.get(0);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertTrue(usedParams.params.getWaitForExecution());
        Assert.assertEquals("step-automation-packages-sample1.jar", new File(usedParams.apFile).getName());
        Assert.assertNull(usedParams.params.getMavenArtifactIdentifier());

        // use maven artifact instead of local file
        remoteExecuteHistory.clear();
        res = runMain(histories, "ap", "execute", "-p=mvn:ch.exense.step:step-automation-packages-junit:0.0.0:tests", "-u=http://localhost:8080");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, remoteExecuteHistory.size());
        usedParams = remoteExecuteHistory.get(0);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertEquals(new MavenArtifactIdentifier("ch.exense.step", "step-automation-packages-junit", "0.0.0", "tests", null), usedParams.params.getMavenArtifactIdentifier());

        // test various report types and output types
        remoteExecuteHistory.clear();
        res = runMain(histories, "ap", "execute", "-p=mvn:ch.exense.step:step-automation-packages-junit:0.0.0:tests", "-u=http://localhost:8080", "--reportType=junit");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, remoteExecuteHistory.size());
        usedParams = remoteExecuteHistory.get(0);
        Assert.assertEquals(1, usedParams.params.getReports().size());
        Assert.assertEquals(AbstractExecuteAutomationPackageTool.ReportType.junit, usedParams.params.getReports().get(0).getReportType());
        Assert.assertEquals(List.of(AbstractExecuteAutomationPackageTool.ReportOutputMode.file), usedParams.params.getReports().get(0).getOutputModes());

        remoteExecuteHistory.clear();
        res = runMain(histories, "ap", "execute", "-p=mvn:ch.exense.step:step-automation-packages-junit:0.0.0:tests", "-u=http://localhost:8080", "--reportType=junit;output=stdout", "--reportType=aggregated;output=file,stdout");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, remoteExecuteHistory.size());
        usedParams = remoteExecuteHistory.get(0);
        Assert.assertEquals(2, usedParams.params.getReports().size());
        Assert.assertEquals(AbstractExecuteAutomationPackageTool.ReportType.junit, usedParams.params.getReports().get(0).getReportType());
        Assert.assertEquals(List.of(AbstractExecuteAutomationPackageTool.ReportOutputMode.stdout), usedParams.params.getReports().get(0).getOutputModes());
        Assert.assertEquals(AbstractExecuteAutomationPackageTool.ReportType.aggregated, usedParams.params.getReports().get(1).getReportType());
        Assert.assertEquals(List.of(AbstractExecuteAutomationPackageTool.ReportOutputMode.file, AbstractExecuteAutomationPackageTool.ReportOutputMode.stdout), usedParams.params.getReports().get(1).getOutputModes());
    }

    @Test
    public void testOutdatedVersion(){
        List<TestApExecuteCommand.RemoteExecutionParams> remoteExecuteHistory = new ArrayList<>();
        List<TestApExecuteCommand.LocalExecutionParams> localExecuteHistory = new ArrayList<>();
        List<TestApDeployCommand.ExecutionParams> deployExecuteHistory = new ArrayList<>();

        Histories histories = new Histories(deployExecuteHistory, remoteExecuteHistory, localExecuteHistory);

        Version actualVersion = Constants.STEP_API_VERSION;
        Version outdatedMajorVersion = new Version(actualVersion.getMajor() - 1, actualVersion.getMinor(), 0);
        Version outdatedMinorVersion = new Version(actualVersion.getMajor(), actualVersion.getMinor() == 0 ? 1 : actualVersion.getMinor() - 1, 0);

        // 1. REMOTE EXECUTION

        // 1.1 validation failed
        int res = runMainWithVersion(histories, outdatedMajorVersion, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080");
        Assert.assertEquals(0, remoteExecuteHistory.size());
        remoteExecuteHistory.clear();

        // 1.2. validation with --force option (execution should be allowed)
        res = runMainWithVersion(histories, outdatedMajorVersion, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080", "--force");
        Assert.assertEquals(1, remoteExecuteHistory.size());
        remoteExecuteHistory.clear();

        // 1.3. minor version mismatch without --force option, validation should fail
        res = runMainWithVersion(histories, outdatedMinorVersion, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080");
        Assert.assertEquals(0, remoteExecuteHistory.size());
        remoteExecuteHistory.clear();

        // 1.4. minor version mismatch with --force option, validation should fail
        res = runMainWithVersion(histories, outdatedMinorVersion, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080", "--force");
        Assert.assertEquals(1, remoteExecuteHistory.size());
        remoteExecuteHistory.clear();

        // 2. LOCAL EXECUTION

        // 2.1 version is not validated for local execution
        res = runMainWithVersion(histories, outdatedMajorVersion, "ap", "execute", "--local", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080");
        Assert.assertEquals(1, localExecuteHistory.size());
        localExecuteHistory.clear();

        // 3. DEPLOY
        res = runMainWithVersion(histories, outdatedMajorVersion, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080");
        Assert.assertEquals(0, deployExecuteHistory.size());
        deployExecuteHistory.clear();

        // 1.2. validation with --force option (execution should be allowed)
        res = runMainWithVersion(histories, outdatedMajorVersion, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080", "--force");
        Assert.assertEquals(1, deployExecuteHistory.size());
        deployExecuteHistory.clear();

        // 1.3. minor version mismatch without --force option (execution should fail)
        res = runMainWithVersion(histories, outdatedMinorVersion, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080");
        Assert.assertEquals(0, deployExecuteHistory.size());
        deployExecuteHistory.clear();

        // 1.4. minor version mismatch with --force option (execution should be allowed)
        res = runMainWithVersion(histories, outdatedMinorVersion, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080", "--force");
        Assert.assertEquals(1, deployExecuteHistory.size());
        deployExecuteHistory.clear();
    }

    @Test
    public void testLocalExecute(){
        List<TestApExecuteCommand.LocalExecutionParams> localExecuteHistory = new ArrayList<>();

        Histories histories = new Histories(null, null, localExecuteHistory);

        // all parameters
        int res = runMain(histories, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "--local", "--includePlans=p1,p2", "--excludePlans=p3,p4", "--includeCategories=CatA,CatB", "--excludeCategories=CatC,CatD","-ep=key1=value1|key2=value2", "-ep=key3=value3");

        Assert.assertEquals(0, res);
        Assert.assertEquals(1, localExecuteHistory.size());
        TestApExecuteCommand.LocalExecutionParams usedParams = localExecuteHistory.get(0);
        Assert.assertEquals("p1,p2", usedParams.includePlans);
        Assert.assertEquals("p3,p4", usedParams.excludePlans);
        Assert.assertEquals("CatA,CatB", usedParams.includeCategories);
        Assert.assertEquals("CatC,CatD", usedParams.excludeCategories);
        Assert.assertEquals(Map.of("key1", "value1", "key2", "value2", "key3", "value3"), usedParams.executionParameters);
        Assert.assertEquals("step-automation-packages-sample1.jar", usedParams.apFile.getName());

        // minimum parameters
        localExecuteHistory.clear();
        res = runMain(histories, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "--local");

        Assert.assertEquals(0, res);
        Assert.assertEquals(1, localExecuteHistory.size());
        usedParams = localExecuteHistory.get(0);
        Assert.assertEquals(Map.of(), usedParams.executionParameters);
        Assert.assertEquals("step-automation-packages-sample1.jar", usedParams.apFile.getName());

        // properties files
        localExecuteHistory.clear();
        res = runMain(histories, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "--local",  "-ep=key1=value1|key2=value2", "-c=src/test/resources/customCli.properties", "-c=src/test/resources/customCli2.properties");

        Assert.assertEquals(0, res);
        Assert.assertEquals(1, localExecuteHistory.size());
        usedParams = localExecuteHistory.get(0);
        Assert.assertEquals(Map.of("key1", "value1", "key2", "value2", "key3", "defaultValue3", "key4", "prioDefaultValue4"), usedParams.executionParameters);
        Assert.assertEquals("step-automation-packages-sample1.jar", usedParams.apFile.getName());
    }

    @Test
    public void testPrepareApFile() throws IOException {
        List<TestApDeployCommand.ExecutionParams> deployExecRegistry = new ArrayList<>();
        List<TestApExecuteCommand.RemoteExecutionParams> remoteExecutionParams = new ArrayList<>();
        List<TestApExecuteCommand.LocalExecutionParams> localExecutionParams = new ArrayList<>();
        Histories histories = new Histories(deployExecRegistry, remoteExecutionParams, localExecutionParams);

        TestApExecuteCommand executeCommand = new TestApExecuteCommand(histories.remoteExecuteHistory, histories.localExecuteHistory);
        TestApDeployCommand deployCommand = new TestApDeployCommand(histories.deployHistory);

        // get test jar copied from step-automation-packages-sample1 maven module
        File testJar = new File("src/test/resources/samples/step-automation-packages-sample1.jar");

        // create the temp folder to unzip the jar into
        File tempFolderIn = FileHelper.createTempFolder("stepcli_test");
        try {
            FileHelper.unzip(testJar, tempFolderIn);

            // use the unzipped jar to prepare the new automation package via 'execute' and 'deploy' commands
            prepareAndVerifyApFile(executeCommand, tempFolderIn);
            prepareAndVerifyApFile(deployCommand, tempFolderIn);
        } finally {
            FileHelper.deleteFolder(tempFolderIn);
        }
    }

    private void prepareAndVerifyApFile(StepConsole.ApCommand.AbstractApCommand executeCommand, File inputFolder) throws IOException {
        File preparedFile = executeCommand.prepareApFile(inputFolder.getAbsolutePath());
        Assert.assertNotNull(preparedFile);

        File tempFolderOut = FileHelper.createTempFolder("stepcli_test");

        try {
            FileHelper.unzip(preparedFile, tempFolderOut);

            Assert.assertTrue(new File(tempFolderOut, "automation-package.yml").exists());
            Assert.assertTrue(new File(tempFolderOut, "keywords.yml").exists());
            Assert.assertTrue(new File(tempFolderOut, "plan.plan").exists());
            Assert.assertTrue(new File(tempFolderOut, "plans").exists());
            Assert.assertTrue(new File(tempFolderOut, "plans/plan1.yml").exists());

            // .apignore file should be ignored
            Assert.assertFalse(new File(tempFolderOut, ".apignore").exists());

            // the files are marked as excluded in .apignore
            Assert.assertFalse(new File(tempFolderOut, "ignored").exists());
            Assert.assertFalse(new File(tempFolderOut, "ignoredFile.yml").exists());
        } finally {
            FileHelper.deleteFolder(tempFolderOut);
        }
    }

    private int runMain(Histories histories, String... args) {
        log.info("--- Run CLI - BEGIN ---");
        int res = StepConsole.executeMain(
                () -> new TestApDeployCommand(histories.deployHistory),
                () -> new TestApExecuteCommand(histories.remoteExecuteHistory, histories.localExecuteHistory),
                false,
                args
        );
        log.info("--- Run CLI - END ---" + "\n");
        return res;
    }

    private int runMainWithVersion(Histories histories, Version version, String... args) {
        log.info("--- Run CLI - BEGIN ---");
        int res = StepConsole.executeMain(
                () -> new TestApDeployCommand(histories.deployHistory, version),
                () -> new TestApExecuteCommand(histories.remoteExecuteHistory, histories.localExecuteHistory, version),
                false,
                args
        );
        log.info("--- Run CLI - END ---" + "\n");
        return res;
    }

    private static class Histories {
        private List<TestApDeployCommand.ExecutionParams> deployHistory;
        private List<TestApExecuteCommand.RemoteExecutionParams> remoteExecuteHistory;
        private List<TestApExecuteCommand.LocalExecutionParams> localExecuteHistory;

        public Histories(List<TestApDeployCommand.ExecutionParams> deployHistory,
                         List<TestApExecuteCommand.RemoteExecutionParams> remoteExecuteHistory,
                         List<TestApExecuteCommand.LocalExecutionParams> localExecuteHistory) {
            this.deployHistory = deployHistory;
            this.remoteExecuteHistory = remoteExecuteHistory;
            this.localExecuteHistory = localExecuteHistory;
        }
    }

    public static class TestApDeployCommand extends StepConsole.ApCommand.ApDeployCommand {

        public final List<ExecutionParams> testRegistry;
        private final Version mockedVersion;

        public TestApDeployCommand(List<ExecutionParams> testRegistry) {
            this.testRegistry = testRegistry;
            this.mockedVersion = null;
        }

        public TestApDeployCommand(List<ExecutionParams> testRegistry, Version mockedVersion) {
            this.testRegistry = testRegistry;
            this.mockedVersion = mockedVersion;
        }

        public static class ExecutionParams {
            private String stepUrl;
            private String projectName;
            private String authToken;
            private boolean async;
            private String apVersion;
            private String activationExpr;
            private String apFile;
            private MavenArtifactIdentifier mavenArtifact;
        }

        @Override
        protected void executeTool(String stepUrl1, String projectName, String authToken1, boolean async, String apVersion, String activationExpr, final MavenArtifactIdentifier mavenArtifact) {
            if (testRegistry != null) {
                ExecutionParams p = new ExecutionParams();
                p.stepUrl = stepUrl1;
                p.projectName = projectName;
                p.authToken = authToken1;
                p.async = async;
                p.apFile = this.apFile;
                p.apVersion = apVersion;
                p.activationExpr = activationExpr;
                p.mavenArtifact = mavenArtifact;
                testRegistry.add(p);
            }
        }

        @Override
        protected Version getVersion() {
            return mockedVersion == null ? super.getVersion() : mockedVersion;
        }

        @Override
        protected ControllerServicesClient createControllerServicesClient() {
            ControllerServicesClient mockedClient = Mockito.mock(ControllerServicesClient.class);
            Mockito.when(mockedClient.getControllerVersion()).thenReturn(Constants.STEP_API_VERSION);
            return mockedClient;
        }
    }

    public static class TestApExecuteCommand extends StepConsole.ApCommand.ApExecuteCommand {

        public final List<RemoteExecutionParams> remoteParams;
        public final List<LocalExecutionParams> localParams;
        private final Version mockedVersion;

        public static class RemoteExecutionParams {
            private String stepUrl;
            private AbstractExecuteAutomationPackageTool.Params params;
            private String apFile;
        }

        public static class LocalExecutionParams {
            private File apFile;
            private String includePlans;
            private String excludePlans;
            private String includeCategories;
            private String excludeCategories;
            private Map<String, String> executionParameters;
        }

        public TestApExecuteCommand(List<RemoteExecutionParams> remoteParams, List<LocalExecutionParams> localParams) {
            this.remoteParams = remoteParams;
            this.localParams = localParams;
            this.mockedVersion = null;
        }

        public TestApExecuteCommand(List<RemoteExecutionParams> remoteParams, List<LocalExecutionParams> localParams, Version mockedVersion) {
            this.remoteParams = remoteParams;
            this.localParams = localParams;
            this.mockedVersion = mockedVersion;
        }

        @Override
        protected void executeRemotely(String stepUrl, AbstractExecuteAutomationPackageTool.Params params) {
            if (remoteParams != null) {
                RemoteExecutionParams p = new RemoteExecutionParams();
                p.stepUrl = stepUrl;
                p.params = params;
                p.apFile = this.apFile;

                remoteParams.add(p);
            }
        }

        @Override
        protected void executeLocally(File file, String includePlans, String excludePlans,
                                      String includeCategories, String excludeCategories, Map<String, String> executionParameters) {
            if (localParams != null) {
                LocalExecutionParams p = new LocalExecutionParams();
                p.apFile = file;
                p.excludePlans = excludePlans;
                p.includePlans = includePlans;
                p.includeCategories = includeCategories;
                p.excludeCategories = excludeCategories;
                p.executionParameters = executionParameters;
                localParams.add(p);
            }
        }

        @Override
        protected Version getVersion() {
            return mockedVersion == null ? super.getVersion() : mockedVersion;
        }

        @Override
        protected ControllerServicesClient createControllerServicesClient() {
            ControllerServicesClient mockedClient = Mockito.mock(ControllerServicesClient.class);
            Mockito.when(mockedClient.getControllerVersion()).thenReturn(Constants.STEP_API_VERSION);
            return mockedClient;
        }
    }

}