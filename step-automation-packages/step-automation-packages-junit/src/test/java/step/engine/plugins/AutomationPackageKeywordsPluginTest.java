package step.engine.plugins;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import step.junit.categories.LocalJMeter;
import step.junit.runner.Step;
import step.junit.runners.annotations.Plans;

@Category(LocalJMeter.class)
@RunWith(Step.class)
@Plans({"testAutomation.plan"})
public class AutomationPackageKeywordsPluginTest {

}