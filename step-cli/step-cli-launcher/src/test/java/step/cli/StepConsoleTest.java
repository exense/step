package step.cli;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
        int res = runMain(histories, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080", "--projectName=testProject", "--token=abc", "--async");

        Assert.assertEquals(0, res);
        Assert.assertEquals(1, deployExecHistory.size());
        TestApDeployCommand.ExecutionParams usedParams = deployExecHistory.get(0);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertEquals("abc", usedParams.authToken);
        Assert.assertEquals("testProject", usedParams.projectName);
        Assert.assertTrue(usedParams.async);
        Assert.assertEquals("step-automation-packages-sample1.jar", usedParams.apFile.getName());

        // for OS (required params only)
        deployExecHistory.clear();
        res = runMain(histories, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, deployExecHistory.size());
        usedParams = deployExecHistory.get(0);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertFalse(usedParams.async);
        Assert.assertEquals("step-automation-packages-sample1.jar", usedParams.apFile.getName());

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
        Assert.assertEquals("step-automation-packages-sample1.jar", usedParams.apFile.getName());

        // several properties files (containing url and project/token)
        deployExecHistory.clear();
        res = runMain(histories, "ap", "deploy", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-c=src/test/resources/customCli.properties", "-c=src/test/resources/customCli2.properties");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, deployExecHistory.size());
        usedParams = deployExecHistory.get(0);
        Assert.assertEquals("http://localhost:8081", usedParams.stepUrl);
        Assert.assertEquals("step-automation-packages-sample1.jar", usedParams.apFile.getName());
        Assert.assertEquals("abc", usedParams.authToken);
        Assert.assertEquals("testProject", usedParams.projectName);
    }

    @Test
    public void testRemoteExecute() {
        List<TestApExecuteCommand.RemoteExecutionParams> remoteExecuteHistory = new ArrayList<>();

        Histories histories = new Histories(null, remoteExecuteHistory, null);

        // all parameters
        int res = runMain(histories, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar",
                "-u=http://localhost:8080", "--projectName=testProject", "--token=abc", "--async", "--includePlans=p1,p2",
                "--executionTimeoutS=1000",
                "--excludePlans=p3,p4", "-ep=key1=value1|key2=value2", "-ep=key3=value3"
        );

        Assert.assertEquals(0, res);
        Assert.assertEquals(1, remoteExecuteHistory.size());
        TestApExecuteCommand.RemoteExecutionParams usedParams = remoteExecuteHistory.get(0);
        Assert.assertEquals("p1,p2", usedParams.includePlans);
        Assert.assertEquals("p3,p4", usedParams.excludePlans);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertEquals("abc", usedParams.authToken);
        Assert.assertEquals("testProject", usedParams.projectName);
        Assert.assertEquals("timeout doesn't match", (Integer) 1000, usedParams.executionTimeoutS);
        Assert.assertTrue(usedParams.async);
        Assert.assertEquals(Map.of("key1", "value1", "key2", "value2", "key3", "value3"), usedParams.executionParameters);
        Assert.assertEquals("step-automation-packages-sample1.jar", usedParams.apFile.getName());

        // minimum parameters
        remoteExecuteHistory.clear();
        res = runMain(histories, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "-u=http://localhost:8080");
        Assert.assertEquals(0, res);
        Assert.assertEquals(1, remoteExecuteHistory.size());
        usedParams = remoteExecuteHistory.get(0);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertFalse(usedParams.async);
        Assert.assertEquals("step-automation-packages-sample1.jar", usedParams.apFile.getName());
    }

    @Test
    public void testLocalExecute(){
        List<TestApExecuteCommand.LocalExecutionParams> localExecuteHistory = new ArrayList<>();

        Histories histories = new Histories(null, null, localExecuteHistory);

        // all parameters
        int res = runMain(histories, "ap", "execute", "-p=src/test/resources/samples/step-automation-packages-sample1.jar", "--local", "--includePlans=p1,p2", "--excludePlans=p3,p4", "-ep=key1=value1|key2=value2", "-ep=key3=value3");

        Assert.assertEquals(0, res);
        Assert.assertEquals(1, localExecuteHistory.size());
        TestApExecuteCommand.LocalExecutionParams usedParams = localExecuteHistory.get(0);
        Assert.assertEquals("p1,p2", usedParams.includePlans);
        Assert.assertEquals("p3,p4", usedParams.excludePlans);
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

        public TestApDeployCommand(List<ExecutionParams> testRegistry) {
            this.testRegistry = testRegistry;
        }

        public static class ExecutionParams {
            private String stepUrl;
            private String projectName;
            private String authToken;
            private boolean async;
            private File apFile;
        }

        @Override
        protected void executeTool(String stepUrl1, String projectName, String authToken1, boolean async1) {
            if (testRegistry != null) {
                ExecutionParams p = new ExecutionParams();
                p.stepUrl = stepUrl1;
                p.projectName = projectName;
                p.authToken = authToken1;
                p.async = async1;
                p.apFile = this.apFile;
                testRegistry.add(p);
            }
        }
    }

    public static class TestApExecuteCommand extends StepConsole.ApCommand.ApExecuteCommand {

        public final List<RemoteExecutionParams> remoteParams;
        public final List<LocalExecutionParams> localParams;

        public static class RemoteExecutionParams {
            private String stepUrl;
            private String projectName;
            private String stepUserId;
            private String authToken;
            private Map<String, String> executionParameters;
            private Integer executionTimeoutS;
            private boolean async;
            private String includePlans;
            private String excludePlans;
            private File apFile;
        }

        public static class LocalExecutionParams {
            private File apFile;
            private String includePlans;
            private String excludePlans;
            private Map<String, String> executionParameters;
        }

        public TestApExecuteCommand(List<RemoteExecutionParams> remoteParams, List<LocalExecutionParams> localParams) {
            this.remoteParams = remoteParams;
            this.localParams = localParams;
        }

        @Override
        protected void executeRemotely(String stepUrl, String projectName, String stepUserId, String authToken, Map<String, String> executionParameters,
                                       Integer executionTimeoutS, boolean async, String includePlans, String excludePlans) {
            if (remoteParams != null) {
                RemoteExecutionParams p = new RemoteExecutionParams();
                p.stepUrl = stepUrl;
                p.projectName = projectName;
                p.authToken = authToken;
                p.async = async;
                p.stepUserId = stepUserId;
                p.executionParameters = executionParameters;
                p.executionTimeoutS = executionTimeoutS;
                p.includePlans = includePlans;
                p.excludePlans = excludePlans;
                p.apFile = this.apFile;
                remoteParams.add(p);
            }
        }

        @Override
        protected void executeLocally(File file, String includePlans, String excludePlans, Map<String, String> executionParameters) {
            if (localParams != null) {
                LocalExecutionParams p = new LocalExecutionParams();
                p.apFile = file;
                p.excludePlans = excludePlans;
                p.includePlans = includePlans;
                p.executionParameters = executionParameters;
                localParams.add(p);
            }
        }
    }

}