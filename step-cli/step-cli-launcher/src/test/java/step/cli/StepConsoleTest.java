package step.cli;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StepConsoleTest {

    @Test
    public void testHelp(){
        List<TestApDeployCommand.ExecutionParams> deployExecRegistry = new ArrayList<>();

        int res = executeMain(deployExecRegistry, "help");
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());

        res = executeMain(deployExecRegistry, "ap", "help");
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());

        res = executeMain(deployExecRegistry, "ap", "deploy", "help");
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());

        res = executeMain(deployExecRegistry, "ap", "execute", "help");
        Assert.assertEquals(0, res);
        Assert.assertEquals(0, deployExecRegistry.size());
    }

    @Test
    public void testDeployAp() {
        List<TestApDeployCommand.ExecutionParams> deployExecRegistry = new ArrayList<>();
        int res = executeMain(deployExecRegistry, "ap", "deploy", "-p=myPackage", "-u=http://localhost:8080", "--projectName=testProject", "--token=abc", "--async");

        Assert.assertEquals(0, res);
        Assert.assertEquals(1, deployExecRegistry.size());
        TestApDeployCommand.ExecutionParams usedParams = deployExecRegistry.get(0);
        Assert.assertEquals("http://localhost:8080", usedParams.stepUrl);
        Assert.assertEquals("abc", usedParams.authToken);
        Assert.assertEquals("testProject", usedParams.projectName);
        Assert.assertTrue(usedParams.async);
    }

    private int executeMain(List<TestApDeployCommand.ExecutionParams> deployExecRegistry, String ... args){
        return StepConsole.executeMain(TestStepConsole::new, TestApCommand::new, () -> new TestApDeployCommand(deployExecRegistry), TestApExecuteCommand::new, args);
    }

    public class TestStepConsole extends StepConsole {

    }

    public class TestApCommand extends StepConsole.ApCommand {

    }

    public static class TestApDeployCommand extends StepConsole.ApCommand.ApDeployCommand {

        public final List<ExecutionParams> testRegistry;

        public TestApDeployCommand(List<ExecutionParams> testRegistry) {
            this.testRegistry = testRegistry;
        }

        public static class ExecutionParams{
            private String stepUrl;
            private String projectName;
            private String authToken;
            private boolean async;
        }

        @Override
        protected void executeTool(String stepUrl1, String projectName, String authToken1, boolean async1) {
            if(testRegistry != null) {
                ExecutionParams p = new ExecutionParams();
                p.stepUrl = stepUrl1;
                p.projectName = projectName;
                p.authToken = authToken1;
                p.async = async1;
                testRegistry.add(p);
            }
        }
    }

    public class TestApExecuteCommand extends StepConsole.ApCommand.ApExecuteCommand {

    }

}