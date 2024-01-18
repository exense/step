package step.automation.packages.junit;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import step.junit.categories.LocalJMeter;
import step.junit.runner.Step;
import step.junit.runners.annotations.Plans;

/**
 * Tests that in plans listed in {@link Plans} we can call the keywords defined in current automation package
 * (i.e. in automation-package.yml descriptor).
 */
@Category(LocalJMeter.class)
@RunWith(Step.class)
@Plans({"testAutomation.plan"})
@AutomationPackagePlans({"testAutomation.plan"})
public class StepAutomationPackagesPlanAnnotation {

}