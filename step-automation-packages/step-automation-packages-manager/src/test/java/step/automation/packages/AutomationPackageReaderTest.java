package step.automation.packages;

import org.junit.Test;
import step.artefacts.TestCase;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.jmeter.automation.JMeterFunctionTestplanConversionRule;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AutomationPackageReaderTest {

    private final AutomationPackageReader reader = new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH);

    @Test
    public void testReadFromPackage() throws AutomationPackageReadingException {
        File automationPackageJar = new File("src/test/resources/samples/step-automation-packages-sample1.jar");

        AutomationPackageContent automationPackageContent = reader.readAutomationPackageFromJarFile(automationPackageJar);
        assertNotNull(automationPackageContent);

        List<AutomationPackageKeyword> keywords = automationPackageContent.getKeywords();
        assertEquals(1, keywords.size());
        AutomationPackageKeyword automationPackageKeyword = keywords.get(0);
        assertEquals(JMeterFunction.class, automationPackageKeyword.getDraftKeyword().getClass());
        assertEquals(
                "jmeterProject1/jmeterProject1.xml",
                automationPackageKeyword.getSpecialAttributes().get(JMeterFunctionTestplanConversionRule.JMETER_TESTPLAN_ATTR)
        );

        List<Plan> plans = automationPackageContent.getPlans();
        assertEquals(1, plans.size());
        assertEquals("Test Plan", plans.get(0).getAttribute(AbstractOrganizableObject.NAME));
        assertEquals(TestCase.class, plans.get(0).getRoot().getClass());
    }

}