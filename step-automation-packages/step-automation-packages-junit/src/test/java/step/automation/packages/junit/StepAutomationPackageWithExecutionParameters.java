package step.automation.packages.junit;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import step.handlers.javahandler.AbstractKeyword;
import step.junit.runner.Step;

@Ignore
@RunWith(Step.class)
@AutomationPackagePlans(value = {"explicitPlanWithSystemProperty", "explicitPlanWithEnvironmentVariable"})
public class StepAutomationPackageWithExecutionParameters extends AbstractKeyword {

    // This test has to be executed with -DSTEP_PARAM_EXEC2=Sysprop1 as system property
    // It is failing otherwise and therefore commented out
    /*
    @Plan
    @Keyword
    public void explicitPlanWithSystemProperty() {
        Assert.assertEquals("Sysprop1", properties.get("PARAM_EXEC2"));
    }
    */

    // This test has to be executed with STEP_PARAM_EXEC3=Envvar1 as environment variable
    // It is failing otherwise and therefore commented out
    /*
    @Plan
    @Keyword
    public void explicitPlanWithEnvironmentVariable() {
        Assert.assertEquals("Envvar1", properties.get("PARAM_EXEC3"));
    }
    */
}