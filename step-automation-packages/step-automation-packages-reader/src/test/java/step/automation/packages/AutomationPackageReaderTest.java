package step.automation.packages;

import org.junit.Test;
import step.artefacts.TestCase;
import step.automation.packages.model.AutomationPackage;
import step.automation.packages.model.AutomationPackageKeyword;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.jmeter.automation.JMeterFunctionTestplanConversionRule;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class AutomationPackageReaderTest {

    private AutomationPackageReader reader = new AutomationPackageReader();

    @Test
    public void testReadFromPackage() throws AutomationPackageReadingException {
        File automationPackageJar = new File("src/test/resources/step/automation/packages/yaml/testPack2.jar");

        AutomationPackage automationPackage = reader.readAutomationPackageFromJarFile(automationPackageJar);
        assertNotNull(automationPackage);

        List<AutomationPackageKeyword> keywords = automationPackage.getKeywords();
        assertEquals(1, keywords.size());
        AutomationPackageKeyword automationPackageKeyword = keywords.get(0);
        assertEquals(JMeterFunction.class, automationPackageKeyword.getDraftKeyword().getClass());
        assertEquals(
                "jmeterProject1/jmeterProject1.xml",
                automationPackageKeyword.getSpecialAttributes().get(JMeterFunctionTestplanConversionRule.JMETER_TESTPLAN_ATTR)
        );

        List<Plan> plans = automationPackage.getPlans();
        assertEquals(1, plans.size());
        assertEquals("Test Plan", plans.get(0).getAttribute(AbstractOrganizableObject.NAME));
        assertEquals(TestCase.class, plans.get(0).getRoot().getClass());
    }

}