package step.automation.packages;

import ch.exense.commons.app.Configuration;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.ForEachBlock;
import step.attachments.FileResolver;
import step.automation.packages.accessor.AutomationPackageAccessorImpl;
import step.automation.packages.scheduler.AutomationPackageSchedulerPlugin;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.controller.ControllerSettingAccessorImpl;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;
import step.core.plans.PlanAccessorImpl;
import step.core.plans.runner.PlanRunnerResult;
import step.core.scheduler.*;
import step.datapool.excel.ExcelDataPool;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.accessor.FunctionAccessorImpl;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.MockedGridClientImpl;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.parameter.ParameterScope;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.GeneralScriptFunctionType;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.jmeter.JMeterFunctionType;
import step.plugins.node.NodeFunction;
import step.plugins.node.NodeFunctionType;
import step.resources.LocalResourceManagerImpl;
import step.resources.Resource;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static step.automation.packages.AutomationPackageLocks.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT;
import static step.automation.packages.AutomationPackageTestUtils.*;

public class AutomationPackageManagerOSTest {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageManagerOSTest.class);

    // how many keywords and plans are defined in original sample
    public static final int KEYWORDS_COUNT = 6;

    // 2 annotated plans and 5 plans from yaml descriptor
    public static final int PLANS_COUNT = 7;
    public static final int SCHEDULES_COUNT = 1;

    // 3 parameters from one fragment and 1 from another
    public static final int PARAMETERS_COUNT = 4;
    private static final String SAMPLE1_FILE_NAME = "step-automation-packages-sample1.jar";
    private static final String SAMPLE1_EXTENDED_FILE_NAME = "step-automation-packages-sample1-extended.jar";
    private static final String SAMPLE_ECHO_FILE_NAME = "step-automation-packages-sample-echo.jar";
    private static final String KW_LIB_FILE_NAME = "step-automation-packages-kw-lib.jar";

    private AutomationPackageManager manager;
    private AutomationPackageAccessorImpl automationPackageAccessor;
    private FunctionManagerImpl functionManager;
    private FunctionAccessorImpl functionAccessor;
    private PlanAccessorImpl planAccessor;
    private LocalResourceManagerImpl resourceManager;
    private ExecutionTaskAccessorImpl executionTaskAccessor;
    private ExecutionScheduler executionScheduler;
    private Accessor<Parameter> parameterAccessor;

    private AutomationPackageLocks automationPackageLocks = new AutomationPackageLocks(AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT);
    private FileResolver fileResolver;

    @Before
    public void before() {
        this.automationPackageAccessor = new AutomationPackageAccessorImpl(new InMemoryCollection<>());
        this.functionAccessor = new FunctionAccessorImpl(new InMemoryCollection<>());
        this.parameterAccessor = new AbstractAccessor<>(new InMemoryCollection<>());
        ParameterManager parameterManager = new ParameterManager(this.parameterAccessor, null, "groovy", new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler())));

        Configuration configuration = createTestConfiguration();
        this.resourceManager = new LocalResourceManagerImpl();
        FunctionTypeRegistry functionTypeRegistry = prepareTestFunctionTypeRegistry(configuration, resourceManager);

        this.functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
        this.planAccessor = new PlanAccessorImpl(new InMemoryCollection<>());

        this.executionTaskAccessor = new ExecutionTaskAccessorImpl(new InMemoryCollection<>());

        // scheduler with mocked executor
        this.executionScheduler = new ExecutionScheduler(new ControllerSettingAccessorImpl(new InMemoryCollection<>()), executionTaskAccessor, Mockito.mock(Executor.class));
        AutomationPackageHookRegistry automationPackageHookRegistry = new AutomationPackageHookRegistry();
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageSchedulerPlugin.registerSchedulerHooks(automationPackageHookRegistry, serializationRegistry, executionScheduler);
        AutomationPackageParametersRegistration.registerParametersHooks(automationPackageHookRegistry, serializationRegistry, parameterManager);

        this.manager = AutomationPackageManager.createMainAutomationPackageManager(
                automationPackageAccessor,
                functionManager,
                functionAccessor,
                planAccessor,
                resourceManager,
                automationPackageHookRegistry,
                new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, automationPackageHookRegistry, serializationRegistry, configuration),
                automationPackageLocks,
                null
        );

        this.manager.setProvidersResolver(new MockedAutomationPackageProvidersResolver(new HashMap<>()));

        this.fileResolver = new FileResolver(resourceManager);
    }

    private static FunctionTypeRegistry prepareTestFunctionTypeRegistry(Configuration configuration, LocalResourceManagerImpl resourceManager) {
        FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(new FileResolver(resourceManager), new MockedGridClientImpl(), new ObjectHookRegistry());
        functionTypeRegistry.registerFunctionType(new JMeterFunctionType(configuration));
        functionTypeRegistry.registerFunctionType(new GeneralScriptFunctionType(configuration));
        functionTypeRegistry.registerFunctionType(new CompositeFunctionType(new ObjectHookRegistry()));
        functionTypeRegistry.registerFunctionType(new NodeFunctionType());

        return functionTypeRegistry;
    }

    private static Configuration createTestConfiguration() {
        return new Configuration();
    }

    @After
    public void after() {
        if (resourceManager != null) {
            resourceManager.cleanup();
        }
    }

    @Test
    public void testCrud() throws IOException {

        // 1. Upload new package
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File extendedAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_EXTENDED_FILE_NAME);

        try (InputStream is = new FileInputStream(automationPackageJar);
             InputStream isExt = new FileInputStream(extendedAutomationPackageJar);
             InputStream isDuplicate = new FileInputStream(automationPackageJar)
        ) {
            AutomationPackageFileSource sample1FileSource = AutomationPackageFileSource.withInputStream(is, SAMPLE1_FILE_NAME);
            AutomationPackageFileSource extendedFileSource = AutomationPackageFileSource.withInputStream(isExt, SAMPLE1_EXTENDED_FILE_NAME);
            AutomationPackageFileSource sample1FileSourceDuplicate = AutomationPackageFileSource.withInputStream(isDuplicate, SAMPLE1_FILE_NAME);

            testCrud(sample1FileSource, extendedFileSource, sample1FileSourceDuplicate);
        }
    }

    private void testCrud(AutomationPackageFileSource sample1FileSource, AutomationPackageFileSource extendedFileSource, AutomationPackageFileSource sample1FileSourceDuplicate) throws IOException {
        Date testStartDate = new Date();

        // upload sample
        SampleUploadingResult r = uploadSample1WithAsserts(sample1FileSource, true, false, false);

        // creation date and user are set
        Date apCreationDate = r.storedPackage.getCreationDate();
        Date currentDate = new Date();
        Assert.assertTrue("Test start date: " + testStartDate.toInstant() + "; Current date: " + currentDate.toInstant() + "; AP creation date: " + apCreationDate.toInstant(),
                !apCreationDate.toInstant().isBefore(testStartDate.toInstant()) && !apCreationDate.toInstant().isAfter(currentDate.toInstant())
        );
        Assert.assertEquals(r.storedPackage.getCreationUser(), "testUser");

        // 2. Update the package - some entities are updated, some entities are added
        AutomationPackageUpdateResult result = manager.createOrUpdateAutomationPackage(
                true, true, null, extendedFileSource,
                null, null, null, null, null, false, "testUser",
                false, true);
        Assert.assertEquals(AutomationPackageUpdateStatus.UPDATED, result.getStatus());
        ObjectId resultId = result.getId();

        // id of existing package is returned
        Assert.assertEquals(r.storedPackage.getId().toString(), resultId.toString());

        // creation date is not changed after update
        AutomationPackage updatedPackage = automationPackageAccessor.get(result.getId());
        Assert.assertEquals(r.storedPackage.getCreationDate().toInstant(), updatedPackage.getCreationDate().toInstant());
        Assert.assertEquals(r.storedPackage.getCreationUser(), updatedPackage.getCreationUser());

        Assert.assertTrue(updatedPackage.getLastModificationDate().after(r.storedPackage.getCreationDate()) && updatedPackage.getLastModificationDate().before(new Date()));
        Assert.assertEquals(updatedPackage.getLastModificationUser(), "testUser");

        r.storedPackage = automationPackageAccessor.get(resultId);
        Assert.assertEquals("My package", r.storedPackage.getAttribute(AbstractOrganizableObject.NAME));

        // 5 plans have been updated, 1 plan has been added
        List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(resultId)).collect(Collectors.toList());
        Assert.assertEquals(PLANS_COUNT + 1, storedPlans.size());

        Plan updatedPlan = storedPlans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(PLAN_NAME_FROM_DESCRIPTOR)).findFirst().orElse(null);
        Plan updatedPlanPlainText = storedPlans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(PLAN_NAME_FROM_DESCRIPTOR_PLAIN_TEXT)).findFirst().orElse(null);
        Assert.assertNotNull(updatedPlan);
        Assert.assertEquals(findPlanByName(r.storedPlans, PLAN_NAME_FROM_DESCRIPTOR).getId(), updatedPlan.getId());
        Assert.assertNotNull(updatedPlanPlainText);
        Assert.assertEquals(findPlanByName(r.storedPlans, PLAN_NAME_FROM_DESCRIPTOR_PLAIN_TEXT).getId(), updatedPlanPlainText.getId());

        Assert.assertNotNull(storedPlans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(PLAN_NAME_FROM_DESCRIPTOR_2)).findFirst().orElse(null));

        // 6 functions have been updated, 1 function has been added
        List<Function> storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(resultId)).collect(Collectors.toList());
        Assert.assertEquals(KEYWORDS_COUNT + 1, storedFunctions.size());

        Function updatedFunction = storedFunctions.stream().filter(f -> f.getAttribute(AbstractOrganizableObject.NAME).equals(J_METER_KEYWORD_1)).findFirst().orElse(null);
        Assert.assertNotNull(updatedFunction);
        Assert.assertEquals(findFunctionByClassAndName(r.storedFunctions, JMeterFunction.class, J_METER_KEYWORD_1).getId(), updatedFunction.getId());

        Assert.assertNotNull(storedFunctions.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(J_METER_KEYWORD_2)).findFirst().orElse(null));

        // 1 task has been updated, 1 task has been added
        List<ExecutiontTaskParameters> storedTasks = executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(resultId)).collect(Collectors.toList());
        Assert.assertEquals(2, storedTasks.size());

        ExecutiontTaskParameters updatedTask = storedTasks.stream().filter(t -> t.getAttribute(AbstractOrganizableObject.NAME).equals(SCHEDULE_1)).findFirst().orElse(null);
        Assert.assertNotNull(updatedTask);
        Assert.assertEquals(r.storedTask.getId(), updatedTask.getId());
        Assert.assertEquals(findPlanByName(r.storedPlans, PLAN_NAME_FROM_DESCRIPTOR).getId().toHexString(), updatedTask.getExecutionsParameters().getRepositoryObject().getRepositoryParameters().get("planid"));

        // new task is configured as inactive in sample
        ExecutiontTaskParameters newTask = storedTasks.stream().filter(t -> t.getAttribute(AbstractOrganizableObject.NAME).equals(SCHEDULE_2)).findFirst().orElse(null);
        Assert.assertNotNull(newTask);
        assertFalse(newTask.isActive());

        // 2 parameter are saved
        List<Parameter> allParameters = parameterAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result.getId())).collect(Collectors.toList());
        Assert.assertEquals(2, allParameters.size());

        Parameter parameter = allParameters.stream().filter(p -> "myKey".equals(p.getKey())).findFirst().orElseThrow();
        assertEquals("myValue", parameter.getValue().get());
        assertEquals("some description", parameter.getDescription());
        assertEquals("abc", parameter.getActivationExpression().getScript());
        assertNull(parameter.getActivationExpression().getScriptEngine());
        assertEquals((Integer) 10, parameter.getPriority());
        assertEquals(true, parameter.getProtectedValue());
        assertEquals(ParameterScope.GLOBAL, parameter.getScope());
        assertEquals(null, parameter.getScopeEntity());

        parameter = allParameters.stream().filter(p -> "myKey2".equals(p.getKey())).findFirst().orElseThrow();
        assertEquals("some description 2", parameter.getDescription());

        // 3. Upload the original sample again - added plans/functions/tasks from step 2 should be removed
        SampleUploadingResult r2 = uploadSample1WithAsserts(sample1FileSourceDuplicate, false, false, false);
        Assert.assertEquals(r.storedPackage.getId(), r2.storedPackage.getId());
        Assert.assertEquals(findPlanByName(r.storedPlans, PLAN_NAME_FROM_DESCRIPTOR), findPlanByName(r2.storedPlans, PLAN_NAME_FROM_DESCRIPTOR));
        Assert.assertEquals(toIds(r.storedFunctions), toIds(r2.storedFunctions));
        Assert.assertEquals(r.storedTask.getId(), r2.storedTask.getId());

        // 4. Delete package by name - everything should be removed
        manager.removeAutomationPackage(r2.storedPackage.getId(), "testUser", null);

        Assert.assertEquals(0, automationPackageAccessor.stream().count());

        Map<String, String> packageIdCriteria = getAutomationPackageIdCriteria(r2.storedPackage.getId());
        Assert.assertEquals(0, planAccessor.findManyByCriteria(packageIdCriteria).count());
        Assert.assertEquals(0, functionAccessor.findManyByCriteria(packageIdCriteria).count());
        Assert.assertEquals(0, executionTaskAccessor.findManyByCriteria(packageIdCriteria).count());
        Assert.assertEquals(0, parameterAccessor.findManyByCriteria(packageIdCriteria).count());
    }

    @Test
    public void testUpdateMetadata() throws IOException {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        try (InputStream is = new FileInputStream(automationPackageJar)) {

            // 1. Upload new package
            SampleUploadingResult r = uploadSample1WithAsserts(AutomationPackageFileSource.withInputStream(is, SAMPLE1_FILE_NAME), true, false, false);

            // 2. Update package metadata - change version
            manager.updateAutomationPackageMetadata(r.storedPackage.getId(), "ver1", null, null);

            AutomationPackage actualAp = automationPackageAccessor.get(r.storedPackage.getId());
            Assert.assertEquals("ver1", actualAp.getVersion());
            Assert.assertEquals("My package.ver1", actualAp.getAttribute(AbstractOrganizableObject.NAME));

            // 3. Update version again and add some activation expression
            manager.updateAutomationPackageMetadata(r.storedPackage.getId(), "ver2", "true == true", null);
            actualAp = automationPackageAccessor.get(r.storedPackage.getId());
            Assert.assertEquals("ver2", actualAp.getVersion());
            Assert.assertEquals("My package.ver2", actualAp.getAttribute(AbstractOrganizableObject.NAME));
            Assert.assertEquals("true == true", actualAp.getActivationExpression().getScript());

            // check that the new activation expression is propagated to all plans and keywords
            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(actualAp.getId())).collect(Collectors.toList());
            Assert.assertEquals(PLANS_COUNT, storedPlans.size());
            for (Plan storedPlan : storedPlans) {
                Assert.assertEquals("true == true", storedPlan.getActivationExpression().getScript());
            }

            List<Function> storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(actualAp.getId())).collect(Collectors.toList());
            Assert.assertEquals(KEYWORDS_COUNT, storedFunctions.size());
            for (Function storedFunction : storedFunctions) {
                Assert.assertEquals("true == true", storedFunction.getActivationExpression().getScript());
            }

            // 4. remove version and activation expression
            manager.updateAutomationPackageMetadata(r.storedPackage.getId(), null, null, null);

            actualAp = automationPackageAccessor.get(r.storedPackage.getId());
            Assert.assertEquals("My package", actualAp.getAttribute(AbstractOrganizableObject.NAME));
            Assert.assertNull(actualAp.getActivationExpression());
            Assert.assertNull(actualAp.getVersion());

            // check that the new activation expression is propagated to all plans and keywords
            storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(actualAp.getId())).collect(Collectors.toList());
            Assert.assertEquals(PLANS_COUNT, storedPlans.size());
            for (Plan storedPlan : storedPlans) {
                Assert.assertNull(storedPlan.getActivationExpression());
            }

            storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(actualAp.getId())).collect(Collectors.toList());
            Assert.assertEquals(KEYWORDS_COUNT, storedFunctions.size());
            for (Function storedFunction : storedFunctions) {
                Assert.assertNull(storedFunction.getActivationExpression());
            }
        }

    }

    @Test
    public void testResourcesInKeywordsAndPlans() throws IOException {
        String fileName = "step-automation-packages-sample2.jar";
        File automationPackageJar = new File("src/test/resources/samples/" + fileName);

        try (InputStream is = new FileInputStream(automationPackageJar)) {
            ObjectId result;
            result = manager.createAutomationPackage(AutomationPackageFileSource.withInputStream(is, fileName), null, null, null, "testUser", false, true, null, null);
            AutomationPackage storedPackage = automationPackageAccessor.get(result);

            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedPlans.size());
            Plan forEachExcelPlan = storedPlans.get(0);
            Assert.assertEquals("Test excel plan", forEachExcelPlan.getAttribute(AbstractOrganizableObject.NAME));
            ForEachBlock forEachArtefact = (ForEachBlock) forEachExcelPlan.getRoot().getChildren().get(0);
            ExcelDataPool excelDataPool = (ExcelDataPool) forEachArtefact.getDataSource();
            checkUploadedResource(excelDataPool.getFile(), "excel1.xlsx");

            List<Function> storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedFunctions.size());
            JMeterFunction jMeterFunction = (JMeterFunction) storedFunctions.get(0);
            DynamicValue<String> jmeterTestplanRef = jMeterFunction.getJmeterTestplan();
            checkUploadedResource(jmeterTestplanRef, "jmeterProject1.xml");
        }
    }

    @Test
    public void testInvalidFile() throws IOException {
        try (InputStream is = new FileInputStream("src/test/resources/step/automation/packages/picture.png")) {
            manager.createAutomationPackage(AutomationPackageFileSource.withInputStream(is, "picture.png"), null, null, null, "testUser", false, true, null, null);
            Assert.fail("The exception should be thrown in case of invalid automation package file");
        } catch (AutomationPackageManagerException ex) {
            // ok - invalid file should cause the exception
        }
    }

    @Test
    public void testZipArchive() throws IOException {
        try (InputStream is = new FileInputStream("src/test/resources/step/automation/packages/step-automation-packages.zip")) {
            ObjectId result;
            result = manager.createAutomationPackage(AutomationPackageFileSource.withInputStream(is, "step-automation-packages.zip"), null, null, null, "testUser", false, true, null, null);
            AutomationPackage storedPackage = automationPackageAccessor.get(result);

            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(4, storedPlans.size());
        }
    }

    @Test
    public void testUpdateAsync() throws IOException, InterruptedException {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);

        try(InputStream is1 = new FileInputStream(automationPackageJar);
            InputStream is2 = new FileInputStream(automationPackageJar);
            InputStream is3 = new FileInputStream(automationPackageJar)) {
            // 1. Upload new package
            SampleUploadingResult r = uploadSample1WithAsserts(AutomationPackageFileSource.withInputStream(is1, SAMPLE1_FILE_NAME), true, true, false);
            uploadSample1WithAsserts(AutomationPackageFileSource.withInputStream(is2, SAMPLE1_FILE_NAME), false, true, false);
            ExecutorService executor = Executors.newFixedThreadPool(1);
            executor.submit(() -> {
                PlanRunnerResult execute = newExecutionEngineBuilder().build().execute(r.storedPlans.get(0));
            });

            //Give some time to let the execution start
            Thread.sleep(500);
            uploadSample1WithAsserts(AutomationPackageFileSource.withInputStream(is3, SAMPLE1_FILE_NAME), false, true, true);
        }
    }

    @Test
    public void testGetAllEntities() throws IOException {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);

        try(InputStream is1 = new FileInputStream(automationPackageJar)) {
            // 1. Upload new package
            SampleUploadingResult r = uploadSample1WithAsserts(AutomationPackageFileSource.withInputStream(is1, SAMPLE1_FILE_NAME), true, false, false);

            // 2. Get all stored entities
            Map<String, List<? extends AbstractOrganizableObject>> allEntities = manager.getAllEntities(r.storedPackage.getId());

            // 3. Compare with expected
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            JsonNode actualJsonNode = objectMapper.valueToTree(allEntities);
            log.info("{}", actualJsonNode);

            Assert.assertEquals(4, allEntities.size());
            List<Function> keywords = (List<Function>) allEntities.get("keywords");
            Assert.assertEquals(KEYWORDS_COUNT, keywords.size());
            findFunctionByClassAndName(keywords, JMeterFunction.class, J_METER_KEYWORD_1);

            List<Plan> plans = (List<Plan>) allEntities.get("plans");
            Assert.assertEquals(PLANS_COUNT, plans.size());
            findPlanByName(plans, PLAN_NAME_FROM_DESCRIPTOR);

            List<ExecutiontTaskParameters> schedules = (List<ExecutiontTaskParameters>) allEntities.get("schedules");
            Assert.assertEquals(SCHEDULES_COUNT, schedules.size());
            findByName(schedules, SCHEDULE_1);

            List<Parameter> parameters = (List<Parameter>) allEntities.get("parameters");
            Assert.assertEquals(PARAMETERS_COUNT, parameters.size());
            Assert.assertTrue(parameters.stream().anyMatch(p -> p.getDescription().equals("some description")));

            // parameter from parameters2.yml
            Assert.assertTrue(parameters.stream().anyMatch(p -> p.getKey().equals("myKey2")));
        }
    }

    @Test
    public void testMavenArtifactsCrud() throws IOException {
        // 1. Upload new package
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File extendedAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_EXTENDED_FILE_NAME);

        MavenArtifactIdentifier sampleIdentifierSnapshot = new MavenArtifactIdentifier("test-group", "ap1", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier extSampleIdentifierSnapshot = new MavenArtifactIdentifier("test-group", "ap1-ext", "1.0.0-SNAPSHOT", null, null);

        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        providersResolver.getMavenArtifactMocks().put(sampleIdentifierSnapshot, automationPackageJar);
        providersResolver.getMavenArtifactMocks().put(extSampleIdentifierSnapshot, extendedAutomationPackageJar);

        AutomationPackageFileSource sampleSource = AutomationPackageFileSource.withMavenIdentifier(sampleIdentifierSnapshot);
        AutomationPackageFileSource extSampleSource = AutomationPackageFileSource.withMavenIdentifier(extSampleIdentifierSnapshot);

        // 2. Test common CRUD scenario
        testCrud(sampleSource, extSampleSource, sampleSource);
    }

    @Test
    public void testResourceCollision(){
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File extendedAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_EXTENDED_FILE_NAME);
        File echoAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE_ECHO_FILE_NAME);
        File kwLibJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);

        MavenArtifactIdentifier sampleSnapshot = new MavenArtifactIdentifier("test-group", "ap1", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier extSampleRelease = new MavenArtifactIdentifier("test-group", "ap1-ext", "1.0.0-RELEASE", null, null);
        MavenArtifactIdentifier echoRelease = new MavenArtifactIdentifier("test-group", "ap1-echo", "1.0.0-RELEASE", null, null);
        MavenArtifactIdentifier echoSnapshot = new MavenArtifactIdentifier("test-group", "ap1-echo", "1.0.0-SNAPSHOT", null, null);

        MavenArtifactIdentifier kwLibSnapshot = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier kwLibRelease = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-RELEASE", null, null);

        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, automationPackageJar);
        providersResolver.getMavenArtifactMocks().put(extSampleRelease, extendedAutomationPackageJar);
        providersResolver.getMavenArtifactMocks().put(echoRelease, echoAutomationPackageJar);
        providersResolver.getMavenArtifactMocks().put(echoSnapshot, echoAutomationPackageJar);

        providersResolver.getMavenArtifactMocks().put(kwLibRelease, kwLibJar);
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot, kwLibJar);

        // upload SNAPSHOT AP (echo) + SNAPSHOT LIB (echo)
        AutomationPackageUpdateResult echoApResult = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(echoSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                null, null, null, null, false, "testUser",
                false, true
        );

        AutomationPackageUpdateResult ap1Result;
        try {
            // try to upload another AP with same snapshot lib - collision should be detected
            manager.createOrUpdateAutomationPackage(
                    true, true, null,
                    AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                    AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                    null, null, null, null, false, "testUser",
                    false, true
            );
            Assert.fail("Exception hasn't been thrown");
        } catch (AutomationPackageCollisionException ex){
            log.info("{}", ex.getMessage());
            Assert.assertTrue(ex.getAutomationPackagesWithSameOrigin().isEmpty());
            Assert.assertEquals(List.of(echoApResult.getId()), ex.getAutomationPackagesWithSameKeywordLib());
        }

        // try again with 'allowUpdateOfOtherPackages' flag
        ap1Result = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                null, null, null, null, false, "testUser",
                true, true
        );
        Assert.assertEquals(List.of(echoApResult.getId()), ap1Result.getConflictingAutomationPackages().getApWithSameKeywordLib());
        Assert.assertTrue(ap1Result.getConflictingAutomationPackages().getApWithSameOrigin().isEmpty());

        // the keyword lib for 'echo' package should be automatically re-uploaded
        AutomationPackage ap1 = automationPackageAccessor.get(ap1Result.getId().toHexString());
        checkResources(ap1, SAMPLE1_FILE_NAME, KW_LIB_FILE_NAME, sampleSnapshot.toStringRepresentation(), kwLibSnapshot.toStringRepresentation());

        AutomationPackage echoAp = automationPackageAccessor.get(echoApResult.getId().toHexString());
        checkResources(echoAp, SAMPLE_ECHO_FILE_NAME, KW_LIB_FILE_NAME, echoSnapshot.toStringRepresentation(), kwLibSnapshot.toStringRepresentation());

        // both automation packages now reference the same keyword lib resource
        assertEquals(ap1.getKeywordLibraryResource(), echoAp.getKeywordLibraryResource());
    }

    @Test
    public void testReuseReleaseResource(){
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File extendedAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_EXTENDED_FILE_NAME);
        File echoAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE_ECHO_FILE_NAME);
        File kwLibJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);

        MavenArtifactIdentifier sampleSnapshot = new MavenArtifactIdentifier("test-group", "ap1", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier extSampleRelease = new MavenArtifactIdentifier("test-group", "ap1-ext", "1.0.0-RELEASE", null, null);
        MavenArtifactIdentifier echoRelease = new MavenArtifactIdentifier("test-group", "ap1-echo", "1.0.0-RELEASE", null, null);
        MavenArtifactIdentifier sameSnapshotForAnotherPackage = new MavenArtifactIdentifier("test-group", "ap1", "1.0.0-SNAPSHOT", null, null);

        MavenArtifactIdentifier kwLibSnapshot = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier kwLibRelease = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-RELEASE", null, null);

        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, automationPackageJar);
        providersResolver.getMavenArtifactMocks().put(extSampleRelease, extendedAutomationPackageJar);
        providersResolver.getMavenArtifactMocks().put(echoRelease, echoAutomationPackageJar);

        providersResolver.getMavenArtifactMocks().put(kwLibRelease, kwLibJar);
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot, kwLibJar);

        // upload SNAPSHOT AP + SNAPSHOT LIB
        AutomationPackageUpdateResult result = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                null, null, null, null, false, "testUser",
                false, true
        );

        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameKeywordLibExists());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameOriginExists());
        AutomationPackage ap1 = automationPackageAccessor.get(result.getId());
        checkResources(ap1, SAMPLE1_FILE_NAME, KW_LIB_FILE_NAME,
                sampleSnapshot.toStringRepresentation(), kwLibSnapshot.toStringRepresentation()
        );

        // upload another AP (echo RELEASE) + RELEASE LIB
        result = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(echoRelease),
                AutomationPackageFileSource.withMavenIdentifier(kwLibRelease),
                null, null, null, null, false, "testUser",
                false, true
        );

        AutomationPackage apEcho = automationPackageAccessor.get(result.getId());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameKeywordLibExists());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameOriginExists());

        checkResources(apEcho, SAMPLE_ECHO_FILE_NAME, KW_LIB_FILE_NAME,
                echoRelease.toStringRepresentation(), kwLibRelease.toStringRepresentation()
        );

        Resource echoReleaseResource = resourceManager.getResource(fileResolver.resolveResourceId(apEcho.getAutomationPackageResource()));
        Resource kwLibReleaseResource = resourceManager.getResource(fileResolver.resolveResourceId(apEcho.getKeywordLibraryResource()));

        // reupload the same AP - existing RELEASE RESOURCES SHOULD BE REUSED
        result = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(echoRelease),
                AutomationPackageFileSource.withMavenIdentifier(kwLibRelease),
                null, null, null, null, false, "testUser",
                false, true
        );
        apEcho = automationPackageAccessor.get(result.getId());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameKeywordLibExists());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameOriginExists());

        Resource echoReleaseResourceAfterUpdate = resourceManager.getResource(fileResolver.resolveResourceId(apEcho.getAutomationPackageResource()));
        Resource kwLibReleaseResourceAfterUpdate = resourceManager.getResource(fileResolver.resolveResourceId(apEcho.getKeywordLibraryResource()));
        Assert.assertEquals(echoReleaseResource.getId(), echoReleaseResourceAfterUpdate.getId());
        Assert.assertEquals(kwLibReleaseResource.getId(), kwLibReleaseResourceAfterUpdate.getId());

        // now we update the first AP - the RELEASE kw lib should be reused without collision
        result = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(extSampleRelease),
                AutomationPackageFileSource.withMavenIdentifier(kwLibRelease),
                null, null, null, null, false, "testUser",
                false, true
        );
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameKeywordLibExists());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameOriginExists());

        ap1 = automationPackageAccessor.get(result.getId());
        checkResources(ap1, SAMPLE1_EXTENDED_FILE_NAME, KW_LIB_FILE_NAME,
                extSampleRelease.toStringRepresentation(), kwLibRelease.toStringRepresentation()
        );
        Resource newKwLibResourceForAp = resourceManager.getResource(fileResolver.resolveResourceId(ap1.getKeywordLibraryResource()));
        Assert.assertEquals(kwLibReleaseResource.getId(), newKwLibResourceForAp.getId());
    }

    private void checkResources(AutomationPackage ap1, String expectedApFileName, String expectedKwFileName,
                                String expectedApOrigin, String expectedKwOrigin) {
        Resource ap1Resource = resourceManager.getResource(fileResolver.resolveResourceId(ap1.getAutomationPackageResource()));
        Resource kwLibResource = resourceManager.getResource(fileResolver.resolveResourceId(ap1.getKeywordLibraryResource()));
        Assert.assertEquals(expectedApFileName, resourceManager.getResourceFile(ap1Resource.getId().toHexString()).getResourceFile().getName());
        Assert.assertEquals(expectedKwFileName, resourceManager.getResourceFile(kwLibResource.getId().toHexString()).getResourceFile().getName());

        Assert.assertEquals(expectedApOrigin, ap1Resource.getOrigin());
        Assert.assertEquals(expectedKwOrigin, kwLibResource.getOrigin());
    }

    private void checkUploadedResource(DynamicValue<String> fileResourceReference, String expectedFileName) {
        FileResolver fileResolver = new FileResolver(resourceManager);
        String resourceReferenceString = fileResourceReference.get();
        Assert.assertTrue(resourceReferenceString.startsWith(FileResolver.RESOURCE_PREFIX));
        String resourceId = fileResolver.resolveResourceId(resourceReferenceString);
        File excelFile = fileResolver.resolve(resourceId);
        Assert.assertNotNull(excelFile);
        Resource resource = resourceManager.getResource(resourceId);
        Assert.assertEquals(expectedFileName, resource.getResourceName());
    }

    private SampleUploadingResult uploadSample1WithAsserts(AutomationPackageFileSource sample1FileSource, boolean createNew, boolean async, boolean expectedDelay) throws IOException {
        FileResolver fileResolver = new FileResolver(resourceManager);

        SampleUploadingResult r = new SampleUploadingResult();

        ObjectId result;
        if (createNew) {
            result = manager.createAutomationPackage(sample1FileSource, null, null, null, "testUser", false, true, null, null);
        } else {
            AutomationPackageUpdateResult updateResult = manager.createOrUpdateAutomationPackage(true, true, null,
                    sample1FileSource,
                    null, null, null, null, null, async, "testUser",
                    false, true);
            if (async && expectedDelay) {
                Assert.assertEquals(AutomationPackageUpdateStatus.UPDATE_DELAYED, updateResult.getStatus());
            } else {
                Assert.assertEquals(AutomationPackageUpdateStatus.UPDATED, updateResult.getStatus());
            }
            result = updateResult.getId();
        }

        r.storedPackage = automationPackageAccessor.get(result);
        Assert.assertEquals("My package", r.storedPackage.getAttribute(AbstractOrganizableObject.NAME));

        log.info("AP resource: {}", r.storedPackage.getAutomationPackageResource());
        Assert.assertNotNull(r.storedPackage.getAutomationPackageResource());

        Resource resourceByAutomationPackage = resourceManager.getResource(fileResolver.resolveResourceId(r.storedPackage.getAutomationPackageResource()));
        if(sample1FileSource.useMavenIdentifier()){
            Assert.assertEquals(
                    sample1FileSource.getMavenArtifactIdentifier().toStringRepresentation(),
                    resourceByAutomationPackage.getOrigin()
            );
        } else {
            Assert.assertEquals("uploaded:", resourceByAutomationPackage.getOrigin());
        }
        Assert.assertEquals(r.storedPackage.getId().toString(), resourceByAutomationPackage.getCustomField("automationPackageId"));

        // upload package without keyword library
        Assert.assertNull(r.storedPackage.getKeywordLibraryResource());

        List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
        Assert.assertEquals(PLANS_COUNT, storedPlans.size());

        r.storedPlans = storedPlans;
        Plan planFromDescriptor = findPlanByName(storedPlans, PLAN_NAME_FROM_DESCRIPTOR);
        Assert.assertNotNull(planFromDescriptor);
        Assert.assertNotNull(findPlanByName(storedPlans, PLAN_FROM_PLANS_ANNOTATION));
        Assert.assertNotNull(findPlanByName(storedPlans, INLINE_PLAN));
        Assert.assertNotNull(findPlanByName(storedPlans, PLAN_NAME_WITH_COMPOSITE));

        r.storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
        Assert.assertEquals(KEYWORDS_COUNT, r.storedFunctions.size());
        findFunctionByClassAndName(r.storedFunctions, JMeterFunction.class, J_METER_KEYWORD_1);
        findFunctionByClassAndName(r.storedFunctions, GeneralScriptFunction.class, ANNOTATED_KEYWORD);
        findFunctionByClassAndName(r.storedFunctions, GeneralScriptFunction.class, INLINE_PLAN);
        findFunctionByClassAndName(r.storedFunctions, NodeFunction.class, NODE_KEYWORD);
        CompositeFunction compositeKeyword = (CompositeFunction) findFunctionByClassAndName(r.storedFunctions, CompositeFunction.class, COMPOSITE_KEYWORD);
        // by default, the 'executeLocally' flag for composite is 'true'
        Assert.assertTrue(compositeKeyword.isExecuteLocally());
        Assert.assertNotNull(compositeKeyword.getPlan());

        // the default plan name is taken from keyword name
        Assert.assertEquals("Composite keyword from AP", compositeKeyword.getPlan().getAttribute(AbstractOrganizableObject.NAME));

        List<ExecutiontTaskParameters> storedTasks = executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
        Assert.assertEquals(SCHEDULES_COUNT, storedTasks.size());
        r.storedTask = storedTasks.get(0);
        Assert.assertEquals(SCHEDULE_1, r.storedTask.getAttribute(AbstractOrganizableObject.NAME));
        Assert.assertEquals("0 15 10 ? * *", r.storedTask.getCronExpression());
        Assert.assertNotNull(r.storedTask.getCronExclusions());
        Assert.assertEquals(List.of("0 0 9 25 * ?", "0 0 9 20 * ?"), r.storedTask.getCronExclusions().stream().map(CronExclusion::getCronExpression).collect(Collectors.toList()));
        Assert.assertTrue(r.storedTask.isActive());
        Assert.assertEquals("local", r.storedTask.getExecutionsParameters().getRepositoryObject().getRepositoryID());
        Assert.assertEquals(planFromDescriptor.getId().toHexString(), r.storedTask.getExecutionsParameters().getRepositoryObject().getRepositoryParameters().get("planid"));

        List<Parameter> allParameters = parameterAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
        Assert.assertEquals(PARAMETERS_COUNT, allParameters.size());
        Parameter parameter = allParameters.get(0);
        assertEquals("myKey", parameter.getKey());
        assertEquals("myValue", parameter.getValue().get());
        assertEquals("some description", parameter.getDescription());
        assertEquals("abc", parameter.getActivationExpression().getScript());
        assertNull(parameter.getActivationExpression().getScriptEngine());
        assertEquals((Integer) 10, parameter.getPriority());
        assertEquals(true, parameter.getProtectedValue());
        assertEquals(ParameterScope.APPLICATION, parameter.getScope());
        assertEquals("entity", parameter.getScopeEntity());

        parameter = allParameters.get(SCHEDULES_COUNT);
        assertEquals("mySimpleKey", parameter.getKey());
        assertFalse(parameter.getValue().isDynamic());
        assertEquals("mySimpleValue", parameter.getValue().get());
        assertEquals(ParameterScope.GLOBAL, parameter.getScope()); // global by default

        parameter = allParameters.get(2);
        assertEquals("myDynamicParam", parameter.getKey());
        assertTrue(parameter.getValue().isDynamic());
        assertEquals("mySimpleKey", parameter.getValue().getExpression());
        assertEquals(ParameterScope.GLOBAL, parameter.getScope()); // global by default

        // parameter from parameters2.yml
        parameter = allParameters.get(3);
        assertEquals("myKey2", parameter.getKey());
        return r;
    }

    private static Map<String, String> getAutomationPackageIdCriteria(ObjectId automationPackageId) {
        Map<String, String> criteria = new HashMap<>();
        criteria.put("customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID, automationPackageId.toString());
        return criteria;
    }

    protected ExecutionEngine.Builder newExecutionEngineBuilder() {
        return ExecutionEngine.builder().withPlugins(List.of(new BaseArtefactPlugin(),
                new AutomationPackageExecutionPlugin(automationPackageLocks),
                new AbstractExecutionEnginePlugin() {
                    @Override
                    public void abortExecution(ExecutionContext context) {
                        try {
                            //delay end of execution to test locks
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }));
    }

    private static class SampleUploadingResult {
        private AutomationPackage storedPackage;
        private List<Plan> storedPlans;
        private List<Function> storedFunctions;
        private ExecutiontTaskParameters storedTask;
    }
}