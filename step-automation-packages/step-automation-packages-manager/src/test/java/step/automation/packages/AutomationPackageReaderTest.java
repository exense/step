package step.automation.packages;

import org.junit.Test;
import step.artefacts.TestCase;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.jmeter.automation.JMeterFunctionTestplanConversionRule;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static step.automation.packages.AutomationPackageTestUtils.*;

public class AutomationPackageReaderTest {

    private final AutomationPackageReader reader = new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH);

    @Test
    public void testReadFromPackage() throws AutomationPackageReadingException {
        File automationPackageJar = new File("src/test/resources/samples/step-automation-packages-sample1.jar");

        AutomationPackageContent automationPackageContent = reader.readAutomationPackageFromJarFile(automationPackageJar);
        assertNotNull(automationPackageContent);

        // 2 keywords: one from descriptor and two from java class with @Keyword annotation
        List<AutomationPackageKeyword> keywords = automationPackageContent.getKeywords();
        assertEquals(3, keywords.size());

        AutomationPackageKeyword jmeterKeyword = AutomationPackageTestUtils.findKeywordByClassAndName(keywords, JMeterFunction.class, J_METER_KEYWORD_1);
        assertEquals(
                "jmeterProject1/jmeterProject1.xml",
                jmeterKeyword.getSpecialAttributes().get(JMeterFunctionTestplanConversionRule.JMETER_TESTPLAN_ATTR)
        );

        AutomationPackageTestUtils.findKeywordByClassAndName(keywords, GeneralScriptFunction.class, ANNOTATED_KEYWORD);
        AutomationPackageTestUtils.findKeywordByClassAndName(keywords, GeneralScriptFunction.class, INLINE_PLAN);

        // 2 annotated plans and 1 plan in yaml descriptor
        List<Plan> plans = automationPackageContent.getPlans();
        assertEquals("Detected plans: " + plans.stream().map(p -> p.getAttribute(AbstractOrganizableObject.NAME)).collect(Collectors.toList()), 3, plans.size());
        assertEquals(TestCase.class, AutomationPackageTestUtils.findPlanByName(plans, PLAN_NAME_FROM_DESCRIPTOR).getRoot().getClass());
        assertEquals(TestCase.class, AutomationPackageTestUtils.findPlanByName(plans, PLAN_FROM_PLANS_ANNOTATION).getRoot().getClass());
        assertEquals(TestCase.class, AutomationPackageTestUtils.findPlanByName(plans, INLINE_PLAN).getRoot().getClass());
    }

}