package step.junit5.runner;

import org.junit.experimental.categories.Category;
import step.automation.packages.junit.IncludePlans;
import step.junit.categories.LocalJMeter;
import step.junit.runners.annotations.Plans;

/**
 * Tests that in plans listed in {@link Plans} we can call the keywords defined in current automation package
 * (i.e. in automation-package.yml descriptor).
 */
@Category(LocalJMeter.class)
@Plans({"testAutomation.plan"})
@IncludePlans({"testAutomation.plan"})
public class StepAutomationPackagesPlanAnnotation extends StepJUnit5 {

}