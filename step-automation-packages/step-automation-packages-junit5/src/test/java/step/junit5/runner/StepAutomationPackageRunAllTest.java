package step.junit5.runner;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Disabled;
import step.junit.categories.LocalJMeter;
import step.junit.runners.annotations.ExecutionParameters;

@Disabled
@Category(LocalJMeter.class)
@ExecutionParameters({"PARAM_EXEC","Value","PARAM_EXEC2","Value","PARAM_EXEC3","Value"})
public class StepAutomationPackageRunAllTest extends StepJUnit5 {

    public void test() {}
}