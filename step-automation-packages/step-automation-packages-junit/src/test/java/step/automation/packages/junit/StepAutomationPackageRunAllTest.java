package step.automation.packages.junit;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import step.junit.categories.LocalJMeter;
import step.junit.runner.Step;
import step.junit.runners.annotations.ExecutionParameters;

@Category(LocalJMeter.class)
@RunWith(Step.class)
@ExecutionParameters({"PARAM_EXEC","Value","PARAM_EXEC2","Value","PARAM_EXEC3","Value"})
public class StepAutomationPackageRunAllTest {

    public void test() {}
}