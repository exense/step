package step.automation.packages.junit;

import org.junit.runner.RunWith;
import step.junit.runner.Step;

@RunWith(Step.class)
@IncludePlans(value = {"Test Plan", "General Script Plan", "Test Plan with Composite", "Plan with Call Plan"})
public class StepAutomationPackageRunSimpleTest {

}