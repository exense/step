package step.junit5.runner;

import org.junit.experimental.categories.Category;
import step.automation.packages.junit.ExcludePlans;
import step.junit.categories.LocalJMeter;
import step.junit.runners.annotations.ExecutionParameters;

@Category(LocalJMeter.class)
@ExecutionParameters({"PARAM_EXEC", "Value", "PARAM_EXEC2", "Value", "PARAM_EXEC3", "Value"})
@ExcludePlans("Plan2") // Need to exclude Plan2 as calling plan2 alone will cause an exception for resolving input
public class StepAutomationPackageRunAllTest extends StepJUnit5 {

    public void test() {
    }
}
