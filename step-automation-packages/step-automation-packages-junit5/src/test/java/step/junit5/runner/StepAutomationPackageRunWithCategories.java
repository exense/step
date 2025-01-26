package step.junit5.runner;

import step.automation.packages.junit.ExcludePlanCategories;
import step.automation.packages.junit.IncludePlanCategories;

@IncludePlanCategories(value = {"My Category A", "My Category B"})
@ExcludePlanCategories(value = {"My Category C", "My Category D"})
public class StepAutomationPackageRunWithCategories extends StepJUnit5 {

}