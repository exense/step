package step.junit5.runner;

import step.automation.packages.junit.IncludePlans;

@IncludePlans(value = {"Test Plan", "General Script Plan", "Test Plan with Composite", "Plan with Call Plan"})
public class StepAutomationPackageRunSimpleTest extends StepJUnit5 {

}