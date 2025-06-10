package step.automation.packages;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import jakarta.json.spi.JsonProvider;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.mockito.Mockito;
import step.artefacts.CallFunction;
import step.artefacts.Sequence;
import step.artefacts.TestCase;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.scheduler.AutomationPackageSchedulerHook;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.automation.AutomationPackageSchedule;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.parameter.ParameterManager;
import step.parameter.ParameterScope;
import step.parameter.automation.AutomationPackageParameter;
import step.parameter.automation.AutomationPackageParameterJsonSchema;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plugins.functions.types.automation.YamlCompositeFunction;
import step.plugins.java.GeneralFunctionScriptLanguage;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.automation.YamlGeneralScriptFunction;
import step.plugins.jmeter.automation.YamlJMeterFunction;
import step.plugins.node.automation.YamlNodeFunction;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
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

        this.reader = new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry, new Configuration());
    }

    @Test
    public void testReadFromPackage() throws AutomationPackageReadingException {
        File automationPackageJar = new File("src/test/resources/samples/step-automation-packages-sample1.jar");

        AutomationPackageContent automationPackageContent = reader.readAutomationPackageFromJarFile(automationPackageJar, null);
        assertNotNull(automationPackageContent);

        // 6 keywords: 4 from descriptor and two from java class with @Keyword annotation
        List<AutomationPackageKeyword> keywords = automationPackageContent.getKeywords();
        assertEquals(6, keywords.size());

        YamlJMeterFunction jmeterKeyword = (YamlJMeterFunction) AutomationPackageTestUtils.findYamlKeywordByClassAndName(keywords, YamlJMeterFunction.class, J_METER_KEYWORD_1);
        assertEquals(
                "jmeterProject1/jmeterProject1.xml",
                jmeterKeyword.getJmeterTestplan().get()
        );

        YamlCompositeFunction compositeKeyword = (YamlCompositeFunction) AutomationPackageTestUtils.findYamlKeywordByClassAndName(keywords, YamlCompositeFunction.class, COMPOSITE_KEYWORD);
        assertEquals(
                "Composite keyword from AP",
                compositeKeyword.getName()
        );

        YamlGeneralScriptFunction generalScriptKeyword = (YamlGeneralScriptFunction) AutomationPackageTestUtils.findYamlKeywordByClassAndName(keywords, YamlGeneralScriptFunction.class, GENERAL_SCRIPT_KEYWORD);
        assertEquals(
                GeneralFunctionScriptLanguage.javascript,
                generalScriptKeyword.getScriptLanguage()
        );
        assertEquals(
                "jsProject/jsSample.js",
                generalScriptKeyword.getScriptFile().get()
        );
        assertEquals(
                "lib/fakeLib.jar",
                generalScriptKeyword.getLibrariesFile().get()
        );

        YamlNodeFunction nodeFunction = (YamlNodeFunction) AutomationPackageTestUtils.findYamlKeywordByClassAndName(keywords, YamlNodeFunction.class, NODE_KEYWORD);
        assertEquals(
                "nodeProject/nodeSample.ts",
                nodeFunction.getJsfile().get()
        );

        GeneralScriptFunction myKeyword2 = (GeneralScriptFunction) findJavaKeywordByClassAndName(keywords, GeneralScriptFunction.class, ANNOTATED_KEYWORD);
        // check the plan-text schema specified in keyword annotation
        assertEquals(JsonProvider.provider().createReader(new StringReader(KEYWORD_SCHEMA_FROM_SAMPLE)).readObject(), myKeyword2.getSchema());

        AutomationPackageTestUtils.findJavaKeywordByClassAndName(keywords, GeneralScriptFunction.class, INLINE_PLAN);

        // 2 annotated plans and 3 plans in yaml descriptor
        List<Plan> plans = automationPackageContent.getPlans();
        assertEquals("Detected plans: " + plans.stream().map(p -> p.getAttribute(AbstractOrganizableObject.NAME)).collect(Collectors.toList()), 5, plans.size());
        Plan testPlan = findPlanByName(plans, PLAN_NAME_FROM_DESCRIPTOR);
        assertEquals(TestCase.class, testPlan.getRoot().getClass());
        assertEquals(TestCase.class, AutomationPackageTestUtils.findPlanByName(plans, PLAN_FROM_PLANS_ANNOTATION).getRoot().getClass());
        assertEquals(TestCase.class, AutomationPackageTestUtils.findPlanByName(plans, INLINE_PLAN).getRoot().getClass());

        // Check plain text plan
        Plan plainTextPlan = findPlanByName(plans, PLAN_NAME_FROM_DESCRIPTOR_PLAIN_TEXT);
        assertEquals(Sequence.class, plainTextPlan.getRoot().getClass());

        //Assert all categories
        Map<String, List<String>> expectedCategoriesByPlan = Map.of(
                "Test Plan", List.of("Yaml Plan"),
                "Test Plan with Composite", List.of("Yaml Plan", "Composite"),
                "plan.plan", List.of("PlainTextPlan", "AnnotatedPlan"),
                "Plain text plan", List.of("PlainTextPlan"),
                "Inline Plan", List.of("InlinePlan", "AnnotatedPlan")
        );
        for (Plan plan : plans) {
            String planName = plan.getAttribute(AbstractOrganizableObject.NAME);
            assertTrue(CollectionUtils.isEqualCollection(expectedCategoriesByPlan.get(planName), plan.getCategories()));
        }

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

        // 3 parameters from one fragment and 1 parameter from another one
        assertEquals(4, parameters.size());
        AutomationPackageParameter parameter = parameters.get(0);
        assertEquals("myKey", parameter.getKey());
        assertEquals("myValue", parameter.getValue().get());
        assertEquals("some description", parameter.getDescription());
        assertEquals("abc", parameter.getActivationScript());
        assertEquals((Integer) 10, parameter.getPriority());
        assertEquals(true, parameter.getProtectedValue());
        assertEquals(ParameterScope.APPLICATION, parameter.getScope());
        assertEquals("entity", parameter.getScopeEntity());

        parameter = parameters.get(1);
        assertEquals("mySimpleKey", parameter.getKey());
        assertFalse(parameter.getValue().isDynamic());
        assertEquals("mySimpleValue", parameter.getValue().get());
        assertEquals(ParameterScope.GLOBAL, parameter.getScope()); // global is default value
        assertEquals(false, parameter.getProtectedValue());

        parameter = parameters.get(2);
        assertEquals("myDynamicParam", parameter.getKey());
        assertTrue(parameter.getValue().isDynamic());
        assertEquals("mySimpleKey", parameter.getValue().getExpression());
        assertEquals(ParameterScope.GLOBAL, parameter.getScope()); // global is default value
        assertEquals(false, parameter.getProtectedValue());

        parameter = parameters.get(3);
        assertEquals("myKey2", parameter.getKey());
    }

    @Test
    public void testFragmentsWithPackageAP() throws AutomationPackageReadingException {
        File automationPackage = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "step/automation/packages/step-automation-packages.zip");

        AutomationPackageContent automationPackageContent = reader.readAutomationPackageFromJarFile(automationPackage, null);
        assertNotNull(automationPackageContent);

        List<Plan> plans = automationPackageContent.getPlans();
        assertEquals(4, plans.size());

        plans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan 1")).findFirst().get();
        plans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan 2")).findFirst().get();
        plans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan 3")).findFirst().get();
        plans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan 4")).findFirst().get();
    }

    @Test
    public void testFragmentsWithExplodedAP() throws AutomationPackageReadingException, IOException {
        File tempFolder = FileHelper.createTempFolder();
        FileHelper.unzip(this.getClass().getClassLoader().getResourceAsStream("step/automation/packages/step-automation-packages.zip"), tempFolder);

        AutomationPackageContent automationPackageContent = reader.readAutomationPackageFromJarFile(tempFolder, null);
        assertNotNull(automationPackageContent);

        List<Plan> plans = automationPackageContent.getPlans();
        assertEquals(4, plans.size());

        plans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan 1")).findFirst().get();
        plans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan 2")).findFirst().get();
        plans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan 3")).findFirst().get();
        plans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan 4")).findFirst().get();
    }

}