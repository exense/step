package step.automation.packages.client;

import org.junit.Test;
import step.automation.packages.execution.AutomationPackageExecutionParameters;
import step.client.credentials.ControllerCredentials;

import java.io.File;

public class RemoteAutomationPackageClientImplTest {

    @Test
    public void createAutomationPackage() {
        RemoteAutomationPackageClientImpl client = new RemoteAutomationPackageClientImpl(new ControllerCredentials("http://localhost:8080", "", ""));
        try {
            client.createAutomationPackage(new File("C:\\Users\\jecom\\Git\\step-samples\\automation-packages\\load-testing-okhttp\\target\\load-testing-okhttp-0.0.0.jar"));
        } catch (AutomationPackageClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createOrUpdateAutomationPackage() {
    }

    @Test
    public void executeAutomationPackage() {
        RemoteAutomationPackageClientImpl client = new RemoteAutomationPackageClientImpl(new ControllerCredentials("http://localhost:8080", "", ""));
        try {
            client.executeAutomationPackage(new File("C:\\Users\\jecom\\Git\\step-samples\\automation-packages\\load-testing-okhttp\\target\\load-testing-okhttp-0.0.0.jar"), new AutomationPackageExecutionParameters());
        } catch (AutomationPackageClientException e) {
            throw new RuntimeException(e);
        }
    }
}