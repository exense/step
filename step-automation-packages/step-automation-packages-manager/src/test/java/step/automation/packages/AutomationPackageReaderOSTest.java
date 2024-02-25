package step.automation.packages;

import jakarta.json.spi.JsonProvider;
import org.junit.Test;
import step.artefacts.CallFunction;
import step.artefacts.TestCase;
import step.automation.packages.model.AutomationPackageContent;
import step.automation.packages.model.AutomationPackageKeyword;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.jmeter.automation.YamlJMeterFunction;

import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static step.automation.packages.AutomationPackageTestUtils.*;

public class AutomationPackageReaderOSTest {

    private static final String KEYWORD_SCHEMA_FROM_SAMPLE = "{ \"properties\": { "
            + "\"myInput\": {\"type\": \"string\", \"default\":\"defaultValueString\"}"
            + "}, \"required\" : []}";

    private final AutomationPackageReaderOS reader = new AutomationPackageReaderOS();

    @Test
    public void testReadFromPackage() throws AutomationPackageReadingException {
        File automationPackageJar = new File("src/test/resources/samples/step-automation-packages-sample1.jar");

        AutomationPackageContent automationPackageContent = reader.readAutomationPackageFromJarFile(automationPackageJar);
        assertNotNull(automationPackageContent);

        // 2 keywords: one from descriptor and two from java class with @Keyword annotation
        List<AutomationPackageKeyword> keywords = automationPackageContent.getKeywords();
        assertEquals(3, keywords.size());

        YamlJMeterFunction jmeterKeyword = (YamlJMeterFunction) AutomationPackageTestUtils.findYamlKeywordByClassAndName(keywords, YamlJMeterFunction.class, J_METER_KEYWORD_1);
        assertEquals(
                "jmeterProject1/jmeterProject1.xml",
                jmeterKeyword.getJmeterTestplan().get()
        );

        GeneralScriptFunction myKeyword2 = (GeneralScriptFunction) findJavaKeywordByClassAndName(keywords, GeneralScriptFunction.class, ANNOTATED_KEYWORD);
        // check the plan-text schema specified in keyword annotation
        assertEquals(JsonProvider.provider().createReader(new StringReader(KEYWORD_SCHEMA_FROM_SAMPLE)).readObject(), myKeyword2.getSchema());

        AutomationPackageTestUtils.findJavaKeywordByClassAndName(keywords, GeneralScriptFunction.class, INLINE_PLAN);

        // 2 annotated plans and 1 plan in yaml descriptor
        List<Plan> plans = automationPackageContent.getPlans();
        assertEquals("Detected plans: " + plans.stream().map(p -> p.getAttribute(AbstractOrganizableObject.NAME)).collect(Collectors.toList()), 3, plans.size());
        Plan testPlan = findPlanByName(plans, PLAN_NAME_FROM_DESCRIPTOR);
        assertEquals(TestCase.class, testPlan.getRoot().getClass());
        assertEquals(TestCase.class, AutomationPackageTestUtils.findPlanByName(plans, PLAN_FROM_PLANS_ANNOTATION).getRoot().getClass());
        assertEquals(TestCase.class, AutomationPackageTestUtils.findPlanByName(plans, INLINE_PLAN).getRoot().getClass());

        // check how keyword inputs from test plan are parsed
        CallFunction callKeyword = (CallFunction) testPlan.getRoot().getChildren()
                .stream()
                .filter(a -> a.getAttribute(AbstractOrganizableObject.NAME).equals("CallMyKeyword2"))
                .findFirst()
                .orElse(null);
        assertNotNull(callKeyword);
        assertFalse(callKeyword.getArgument().isDynamic());
        assertEquals("{\"myInput\":{\"dynamic\":false,\"value\":\"myValue\"}}", callKeyword.getArgument().get());
    }

}