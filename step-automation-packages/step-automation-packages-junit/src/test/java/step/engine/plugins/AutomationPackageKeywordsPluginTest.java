package step.engine.plugins;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import step.junit.runner.Step;
import step.junit.runners.annotations.Plans;

// TODO: how to disable the test if jmeter_home env variable is undefined?
@Ignore
@RunWith(Step.class)
@Plans({"testAutomation.plan"})
public class AutomationPackageKeywordsPluginTest {

}