package step.automation.packages;

import jakarta.json.spi.JsonProvider;
import org.junit.Test;
import org.mockito.Mockito;
import step.artefacts.CallFunction;
import step.artefacts.TestCase;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.scheduler.AutomationPackageSchedulerHook;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessor;
import step.core.scheduler.automation.AutomationPackageSchedule;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.parameter.ParameterManager;
import step.parameter.ParameterScope;
import step.parameter.automation.AutomationPackageParameter;
import step.parameter.automation.AutomationPackageParameterJsonSchema;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.jmeter.automation.YamlJMeterFunction;

import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static step.automation.packages.AutomationPackageTestUtils.*;

public class AutomationPackageReaderTest {

    private static final String KEYWORD_SCHEMA_FROM_SAMPLE = "{\"type\":\"object\", \"properties\": { "
            + "\"myInput\": {\"type\": \"string\", \"default\":\"defaultValueString\"}"
            + "}, \"required\" : []}";

    private final AutomationPackageReader reader;

    public AutomationPackageReaderTest() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();

        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);

        hookRegistry.register(AutomationPackageSchedule.FIELD_NAME_IN_AP, new AutomationPackageSchedulerHook(Mockito.mock(ExecutionScheduler.class)));

        // accessor is not required in this test - we only read the yaml and don't store the result anywhere
        AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, Mockito.mock(ParameterManager.class));

        this.reader = new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry);
    }

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

        // 1 parameter
        List<AutomationPackageParameter> parameters = (List<AutomationPackageParameter>) automationPackageContent.getAdditionalData().get(AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP);
        assertNotNull(parameters);
        assertEquals(2, parameters.size());
        AutomationPackageParameter parameter = parameters.get(0);
        assertEquals("myKey", parameter.getKey());
        assertEquals("myValue", parameter.getValue());
        assertEquals("some description", parameter.getDescription());
        assertEquals("abc", parameter.getActivationScript());
        assertEquals((Integer) 10, parameter.getPriority());
        assertEquals(true, parameter.getProtectedValue());
        assertEquals(ParameterScope.APPLICATION, parameter.getScope());
        assertEquals("entity", parameter.getScopeEntity());
        parameter = parameters.get(1);
        assertEquals("mySimpleKey", parameter.getKey());
        assertEquals("mySimpleValue", parameter.getValue());
        assertEquals(ParameterScope.GLOBAL, parameter.getScope()); // global is default value
        assertEquals(false, parameter.getProtectedValue());
    }

}