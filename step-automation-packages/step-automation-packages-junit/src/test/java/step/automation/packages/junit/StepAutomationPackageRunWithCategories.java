package step.automation.packages.junit;

import org.junit.runner.RunWith;
import step.junit.runner.Step;

@RunWith(Step.class)
@IncludePlanCategories(value = {"My Category A", "My Category B"})
@ExcludePlanCategories(value = {"My Category C", "My Category D"})
public class StepAutomationPackageRunWithCategories {

}