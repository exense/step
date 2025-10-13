package step.automation.packages;

import ch.exense.commons.app.Configuration;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.*;
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
import step.repositories.artifact.ResolvedMavenArtifact;
import step.repositories.artifact.SnapshotMetadata;
import step.resources.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static step.automation.packages.AutomationPackageLocks.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT;
import static step.automation.packages.AutomationPackageTestUtils.*;

public class AutomationPackageManagerOSTest {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageManagerOSTest.class);

    // how many keywords and plans are defined in original sample
    public static final int KEYWORDS_COUNT = 8;

    // 2 annotated plans and 5 plans from yaml descriptor
    public static final int PLANS_COUNT = 7;
    public static final int SCHEDULES_COUNT = 1;

    // 3 parameters from one fragment and 1 from another
    public static final int PARAMETERS_COUNT = 4;
    private static final String SAMPLE1_FILE_NAME = "step-automation-packages-sample1.jar";
    private static final String SAMPLE1_EXTENDED_FILE_NAME = "step-automation-packages-sample1-extended.jar";
    private static final String SAMPLE_ECHO_FILE_NAME = "step-automation-packages-sample-echo.jar";
    private static final String KW_LIB_CALL_FILE_NAME = "step-automation-packages-kw-lib-call.jar";

    private static final String KW_LIB_FILE_NAME = "step-automation-packages-kw-lib.jar";
    private static final String KW_LIB_FILE_UPDATED_NAME = "step-automation-packages-kw-lib-updated.jar";
    private static final String KW_LIB_FILE_RELEASE_NAME = "step-automation-packages-kw-lib-release.jar";

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

        this.manager.setProvidersResolver(new MockedAutomationPackageProvidersResolver(new HashMap<>(), resourceManager));

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
                null, null, null, null, o -> true, o -> true, false,
                "testUser", false, true);
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
        manager.removeAutomationPackage(r2.storedPackage.getId(), "testUser", null, null);

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
            manager.updateAutomationPackageMetadata(r.storedPackage.getId(), "ver1", null, null, null);

            AutomationPackage actualAp = automationPackageAccessor.get(r.storedPackage.getId());
            Assert.assertEquals("ver1", actualAp.getVersion());
            Assert.assertEquals("My package.ver1", actualAp.getAttribute(AbstractOrganizableObject.NAME));

            // 3. Update version again and add some activation expression
            manager.updateAutomationPackageMetadata(r.storedPackage.getId(), "ver2", "true == true", null, null);
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
            manager.updateAutomationPackageMetadata(r.storedPackage.getId(), null, null, null, null);

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
            result = manager.createAutomationPackage(AutomationPackageFileSource.withInputStream(is, fileName), null, null, null, "testUser", false, true, null, o -> true, o -> true);

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
            manager.createAutomationPackage(AutomationPackageFileSource.withInputStream(is, "picture.png"), null, null, null, "testUser", false, true, null, o -> true, o -> true);
            Assert.fail("The exception should be thrown in case of invalid automation package file");
        } catch (AutomationPackageManagerException ex) {
            // ok - invalid file should cause the exception
        }
    }

    @Test
    public void testZipArchive() throws IOException {
        try (InputStream is = new FileInputStream("src/test/resources/step/automation/packages/step-automation-packages.zip")) {
            ObjectId result;
            result = manager.createAutomationPackage(AutomationPackageFileSource.withInputStream(is, "step-automation-packages.zip"), null, null, null, "testUser", false, true, null, o -> true, o -> true);

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
        providersResolver.getMavenArtifactMocks().put(sampleIdentifierSnapshot, new ResolvedMavenArtifact(automationPackageJar, null));
        providersResolver.getMavenArtifactMocks().put(extSampleIdentifierSnapshot, new ResolvedMavenArtifact(extendedAutomationPackageJar, null));

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
        File kwLibSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);
        File kwLibReleaseJar = new File("src/test/resources/samples/" + KW_LIB_FILE_RELEASE_NAME);

        MavenArtifactIdentifier sampleSnapshot = new MavenArtifactIdentifier("test-group", "ap1", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier extSampleRelease = new MavenArtifactIdentifier("test-group", "ap1-ext", "1.0.0-RELEASE", null, null);
        MavenArtifactIdentifier echoRelease = new MavenArtifactIdentifier("test-group", "ap1-echo", "1.0.0-RELEASE", null, null);
        MavenArtifactIdentifier echoSnapshot = new MavenArtifactIdentifier("test-group", "ap1-echo", "1.0.0-SNAPSHOT", null, null);

        MavenArtifactIdentifier kwLibSnapshot = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier kwLibRelease = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-RELEASE", null, null);

        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        long now = System.currentTimeMillis();
        SnapshotMetadata snapshotMetadata = new SnapshotMetadata("some timestamp", now, 1, true);
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, new ResolvedMavenArtifact(automationPackageJar, snapshotMetadata));
        providersResolver.getMavenArtifactMocks().put(extSampleRelease, new ResolvedMavenArtifact(extendedAutomationPackageJar, null));
        providersResolver.getMavenArtifactMocks().put(echoRelease, new ResolvedMavenArtifact(echoAutomationPackageJar, null));
        providersResolver.getMavenArtifactMocks().put(echoSnapshot, new ResolvedMavenArtifact(echoAutomationPackageJar, snapshotMetadata));

        providersResolver.getMavenArtifactMocks().put(kwLibRelease, new ResolvedMavenArtifact(kwLibReleaseJar, null));
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot,new ResolvedMavenArtifact( kwLibSnapshotJar, snapshotMetadata));

        // upload SNAPSHOT AP (echo) + SNAPSHOT LIB (echo)
        AutomationPackageUpdateResult echoApResult = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(echoSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                null, null, null, o -> true, o -> true, false,
                "testUser", false,
                true);

        AutomationPackageUpdateResult ap1Result;
        try {
            // try to upload another AP with same snapshot lib - collision should be detected
            manager.createOrUpdateAutomationPackage(
                    true, true, null,
                    AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                    AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                    null, null, null, o -> true, o -> true, false,
                    "testUser", false,
                    true);
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
                null, null, null, o -> true, o -> true, false,
                "testUser", true,
                true);
        Assert.assertEquals(List.of(echoApResult.getId()), ap1Result.getConflictingAutomationPackages().getApWithSameLibrary());
        Assert.assertTrue(ap1Result.getConflictingAutomationPackages().getApWithSameOrigin().isEmpty());

        // the keyword lib for 'echo' package should be automatically re-uploaded
        AutomationPackage ap1 = automationPackageAccessor.get(ap1Result.getId().toHexString());
        checkResources(ap1, SAMPLE1_FILE_NAME, KW_LIB_FILE_NAME, sampleSnapshot.toStringRepresentation(), kwLibSnapshot.toStringRepresentation());

        AutomationPackage echoAp = automationPackageAccessor.get(echoApResult.getId().toHexString());
        checkResources(echoAp, SAMPLE_ECHO_FILE_NAME, KW_LIB_FILE_NAME, echoSnapshot.toStringRepresentation(), kwLibSnapshot.toStringRepresentation());

        // both automation packages now reference the same keyword lib resource
        assertEquals(ap1.getAutomationPackageLibraryResource(), echoAp.getAutomationPackageLibraryResource());
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
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, new ResolvedMavenArtifact(automationPackageJar, null));
        providersResolver.getMavenArtifactMocks().put(extSampleRelease, new ResolvedMavenArtifact(extendedAutomationPackageJar, null));
        providersResolver.getMavenArtifactMocks().put(echoRelease, new ResolvedMavenArtifact(echoAutomationPackageJar, null));

        providersResolver.getMavenArtifactMocks().put(kwLibRelease, new ResolvedMavenArtifact(kwLibJar, null));
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot, new ResolvedMavenArtifact(kwLibJar, null));

        // upload SNAPSHOT AP + SNAPSHOT LIB
        AutomationPackageUpdateResult result = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                null, null, null, o -> true, o -> true, false,
                "testUser", false,
                true);

        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameLibraryExists());
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
                null, null, null, o -> true, o -> true, false,
                "testUser", false,
                true);

        AutomationPackage apEcho = automationPackageAccessor.get(result.getId());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameLibraryExists());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameOriginExists());

        checkResources(apEcho, SAMPLE_ECHO_FILE_NAME, KW_LIB_FILE_NAME,
                echoRelease.toStringRepresentation(), kwLibRelease.toStringRepresentation()
        );

        Resource echoReleaseResource = resourceManager.getResource(FileResolver.resolveResourceId(apEcho.getAutomationPackageResource()));
        Resource kwLibReleaseResource = resourceManager.getResource(FileResolver.resolveResourceId(apEcho.getAutomationPackageLibraryResource()));

        // reupload the same AP - existing RELEASE RESOURCES SHOULD BE REUSED
        result = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(echoRelease),
                AutomationPackageFileSource.withMavenIdentifier(kwLibRelease),
                null, null, null, o -> true, o -> true, false,
                "testUser", false,
                true);
        apEcho = automationPackageAccessor.get(result.getId());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameLibraryExists());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameOriginExists());

        Resource echoReleaseResourceAfterUpdate = resourceManager.getResource(FileResolver.resolveResourceId(apEcho.getAutomationPackageResource()));
        Resource kwLibReleaseResourceAfterUpdate = resourceManager.getResource(FileResolver.resolveResourceId(apEcho.getAutomationPackageLibraryResource()));
        Assert.assertEquals(echoReleaseResource.getId(), echoReleaseResourceAfterUpdate.getId());
        Assert.assertEquals(kwLibReleaseResource.getId(), kwLibReleaseResourceAfterUpdate.getId());

        // now we update the first AP - the RELEASE kw lib should be reused without collision
        result = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(extSampleRelease),
                AutomationPackageFileSource.withMavenIdentifier(kwLibRelease),
                null, null, null, o -> true, o -> true, false,
                "testUser", false,
                true);
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameLibraryExists());
        Assert.assertFalse(result.getConflictingAutomationPackages().apWithSameOriginExists());

        ap1 = automationPackageAccessor.get(result.getId());
        checkResources(ap1, SAMPLE1_EXTENDED_FILE_NAME, KW_LIB_FILE_NAME,
                extSampleRelease.toStringRepresentation(), kwLibRelease.toStringRepresentation()
        );
        Resource newKwLibResourceForAp = resourceManager.getResource(FileResolver.resolveResourceId(ap1.getAutomationPackageLibraryResource()));
        Assert.assertEquals(kwLibReleaseResource.getId(), newKwLibResourceForAp.getId());
    }

    @Test
    public void testKwLibVersioning(){
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File echoAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE_ECHO_FILE_NAME);
        File kwLibSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);
        File kwLibUpdatedSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_UPDATED_NAME);

        MavenArtifactIdentifier sampleSnapshot = new MavenArtifactIdentifier("test-group", "ap1", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier echoRelease = new MavenArtifactIdentifier("test-group", "ap1-echo", "1.0.0-RELEASE", null, null);
        MavenArtifactIdentifier kwLibSnapshot = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-SNAPSHOT", null, null);

        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        long now = System.currentTimeMillis();
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, new ResolvedMavenArtifact(automationPackageJar, new SnapshotMetadata("some timestamp", now, 1, true)));
        providersResolver.getMavenArtifactMocks().put(echoRelease, new ResolvedMavenArtifact(echoAutomationPackageJar, new SnapshotMetadata("some timestamp", now, 1, true)));

        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot,new ResolvedMavenArtifact( kwLibSnapshotJar, new SnapshotMetadata("some timestamp", now, 1, true)));

        // upload echo AP (echo RELEASE) + SNAPSHOT LIB
        AutomationPackageUpdateResult resultEcho = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(echoRelease),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                null, null, null, o -> true, o -> true, false,
                "testUser", false,
                true);
        log.info("Echo AP: {}", resultEcho.getId());

        // upload main AP (sample SNAPSHOT) + SNAPSHOT LIB - VERSION 1 (WITHOUT CHECK FOR DUPLICATES)
        AutomationPackageUpdateResult resultV1 = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                "v1", null, null, o -> true, o -> true, false,
                "testUser", false,
                false);
        log.info("AP v1: {}", resultV1.getId());

        // imitate the snapshot update
        long now2 = System.currentTimeMillis();
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot, new ResolvedMavenArtifact(kwLibUpdatedSnapshotJar, new SnapshotMetadata("some timestamp", now2, 1, true)));

        //imitate the new fetch of metadata for the AP sample (no new version available)
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, new ResolvedMavenArtifact(automationPackageJar, new SnapshotMetadata("some timestamp", now2, 1, true)));

        // upload main AP (sample SNAPSHOT) + UPDATED SNAPSHOT LIB - VERSION 2 (WITH CHECK FOR DUPLICATES)
        try {
            manager.createOrUpdateAutomationPackage(
                    true, true, null,
                    AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                    AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                    "v2", null, null, o -> true, o -> true, false,
                    "testUser", false,
                    true);
            fail();
        } catch (AutomationPackageCollisionException ex){
            // both packages reuse the same keyword lib
            Assert.assertEquals(Set.of(resultV1.getId(), resultEcho.getId()), new HashSet<>(ex.getAutomationPackagesWithSameKeywordLib()));

            // v1 reuses the same AP artifact (SNAPSHOT) which was not modified
            Assert.assertEquals(Set.of(resultV1.getId()), new HashSet<>(ex.getAutomationPackagesWithSameOrigin()));
        }

        // upload main AP (sample SNAPSHOT) + UPDATED SNAPSHOT LIB - VERSION 2 (allow update of other packages)
        AutomationPackageUpdateResult resultV2 = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                "v2", null, null, o -> true, o -> true, false,
                "testUser", true,
                true);

        // there is no exception, but we generate warning messages about APs sharing the same resources
        // both packages reuse the same keyword lib
        Assert.assertEquals(Set.of(resultV1.getId(), resultEcho.getId()), new HashSet<>(resultV2.getConflictingAutomationPackages().getApWithSameLibrary()));

        // v1 reuses the same AP artifact (SNAPSHOT)
        Assert.assertEquals(Set.of(resultV1.getId()), new HashSet<>(resultV2.getConflictingAutomationPackages().getApWithSameOrigin()));

        // v1 and v2 reuse the actual (updated) snapshot lib and the sampleSnapshot (main ap file)
        AutomationPackage apVer1 = automationPackageAccessor.get(resultV1.getId());
        AutomationPackage apVer2 = automationPackageAccessor.get(resultV2.getId());
        AutomationPackage apEcho = automationPackageAccessor.get(resultEcho.getId());

        Resource apV2KeywordResource = resourceManager.getResource(FileResolver.resolveResourceId(apVer2.getAutomationPackageLibraryResource()));
        Assert.assertEquals(kwLibSnapshot.toStringRepresentation(), apV2KeywordResource.getOrigin());
        Assert.assertEquals(apVer1.getAutomationPackageLibraryResource(), apVer2.getAutomationPackageLibraryResource());
        Assert.assertEquals(apVer1.getAutomationPackageLibraryResource(), apEcho.getAutomationPackageLibraryResource());
        ResourceRevisionFileHandle kwLibRevision = resourceManager.getResourceFile(FileResolver.resolveResourceId(apVer2.getAutomationPackageLibraryResource()));
        Assert.assertEquals(KW_LIB_FILE_UPDATED_NAME, kwLibRevision.getResourceFile().getName());

        Resource apV2Resource = resourceManager.getResource(FileResolver.resolveResourceId(apVer2.getAutomationPackageResource()));
        ResourceRevisionFileHandle apV2Revision = resourceManager.getResourceFile(FileResolver.resolveResourceId(apVer2.getAutomationPackageResource()));
        Assert.assertEquals(apV2Resource.getOrigin(), sampleSnapshot.toStringRepresentation());
        Assert.assertEquals(apVer1.getAutomationPackageResource(), apVer2.getAutomationPackageResource());
        Assert.assertEquals(SAMPLE1_FILE_NAME, apV2Revision.getResourceFile().getName());

        // check that the main file for AP Echo is not touched
        ResourceRevisionFileHandle echoResourceRevision = resourceManager.getResourceFile(FileResolver.resolveResourceId(apEcho.getAutomationPackageResource()));
        Assert.assertEquals(SAMPLE_ECHO_FILE_NAME, echoResourceRevision.getResourceFile().getName());
        //Check the echo KW lib point to the new SNAPSHOT
        ResourceRevisionFileHandle kwLibRevisionEcho = resourceManager.getResourceFile(FileResolver.resolveResourceId(apEcho.getAutomationPackageLibraryResource()));
        Assert.assertEquals(KW_LIB_FILE_UPDATED_NAME, kwLibRevisionEcho.getResourceFile().getName());
    }

    @Test
    public void testApSnapshotReupload() throws IOException, InterruptedException {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File updatedAutomationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_EXTENDED_FILE_NAME);
        File kwLibSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);

        MavenArtifactIdentifier sampleSnapshot = new MavenArtifactIdentifier("test-group", "ap1", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier kwLibSnapshot = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-SNAPSHOT", null, null);

        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        long now = System.currentTimeMillis();
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, new ResolvedMavenArtifact(automationPackageJar, new SnapshotMetadata("some timestamp", now, 1, true)));
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot, new ResolvedMavenArtifact(kwLibSnapshotJar, new SnapshotMetadata("some timestamp", now, 1, true)));

        // upload main AP (sample SNAPSHOT) + SNAPSHOT LIB - VERSION 1
        AutomationPackageUpdateResult result1 = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                "v1", null, null, o -> true, o -> true, false,
                "testUser", true,
                true);

        // check used AP resource
        AutomationPackage ap1 = automationPackageAccessor.get(result1.getId());
        ResourceRevisionFileHandle ap1Revision = resourceManager.getResourceFile(FileResolver.resolveResourceId(ap1.getAutomationPackageResource()));
        Assert.assertArrayEquals(Files.readAllBytes(automationPackageJar.toPath()), Files.readAllBytes(ap1Revision.getResourceFile().toPath()));

        // UPDATE THE SNAPSHOT CONTENT IN MAVEN !!!
        now = System.currentTimeMillis();
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, new ResolvedMavenArtifact(updatedAutomationPackageJar, new SnapshotMetadata("some timestamp", now, 1, true)));

        // reupload main AP (sample SNAPSHOT) + SNAPSHOT LIB - with the same VERSION 1
        AutomationPackageUpdateResult result2 = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                "v1", null, null, o -> true, o -> true, false,
                "testUser", true,
                true);
        AutomationPackage ap2 = automationPackageAccessor.get(result2.getId());

        // the automation package should use updated snapshot content
        ResourceRevisionFileHandle ap2Revision = resourceManager.getResourceFile(FileResolver.resolveResourceId(ap2.getAutomationPackageResource()));
        Assert.assertArrayEquals(Files.readAllBytes(updatedAutomationPackageJar.toPath()), Files.readAllBytes(ap2Revision.getResourceFile().toPath()));

        // the resource id should NOT be changed, because we reuploaded the snapshot with same resource id
        Assert.assertEquals(ap1.getAutomationPackageResource(), ap2.getAutomationPackageResource());

        // automation packages entities should be taken from updated snapshot
        List<Function> storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(ap2.getId())).collect(Collectors.toList());
        Assert.assertEquals(KEYWORDS_COUNT + 1, storedFunctions.size());

        Function updatedFunction = storedFunctions.stream().filter(f -> f.getAttribute(AbstractOrganizableObject.NAME).equals(J_METER_KEYWORD_1)).findFirst().orElse(null);
        Assert.assertNotNull(updatedFunction);

        checkResourceCleanup(FileResolver.resolveResourceId(ap2.getAutomationPackageResource()), ap2Revision, null, null);
    }

    @Test
    public void testKeywordLibClassLoader(){
        File kwLibCallApJar = new File("src/test/resources/samples/" + KW_LIB_CALL_FILE_NAME);
        File kwLibSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);

        MavenArtifactIdentifier sampleSnapshot = new MavenArtifactIdentifier("test-group", "test-kw-lib-call", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier kwLibSnapshot = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-SNAPSHOT", null, null);

        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, new ResolvedMavenArtifact(kwLibCallApJar, null));
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot, new ResolvedMavenArtifact(kwLibSnapshotJar, null));

        // upload main AP (sample SNAPSHOT using classes from LIB) + SNAPSHOT LIB
        AutomationPackageUpdateResult result = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                null, null, null, o -> true, o -> true, false,
                "testUser", true,
                true);

        // deploy should not fail
        Assert.assertEquals(AutomationPackageUpdateStatus.CREATED, result.getStatus());
    }

    @Test
    public void testReuseApByResourceId() throws IOException, InterruptedException {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File kwLibSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);

        MavenArtifactIdentifier sampleSnapshot = new MavenArtifactIdentifier("test-group", "ap1", "1.0.0-SNAPSHOT", null, null);
        MavenArtifactIdentifier kwLibSnapshot = new MavenArtifactIdentifier("test-group", "test-kw-lib", "1.0.0-SNAPSHOT", null, null);

        MockedAutomationPackageProvidersResolver providersResolver = (MockedAutomationPackageProvidersResolver) manager.getProvidersResolver();
        providersResolver.getMavenArtifactMocks().put(sampleSnapshot, new ResolvedMavenArtifact(automationPackageJar, null));
        providersResolver.getMavenArtifactMocks().put(kwLibSnapshot, new ResolvedMavenArtifact(kwLibSnapshotJar, null));

        // upload main AP (sample SNAPSHOT) + SNAPSHOT LIB - VERSION 1
        AutomationPackageUpdateResult result1 = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withMavenIdentifier(sampleSnapshot),
                AutomationPackageFileSource.withMavenIdentifier(kwLibSnapshot),
                "v1", null, null, o -> true, o -> true, false,
                "testUser", true,
                true);

        // check used AP resource
        AutomationPackage ap1 = automationPackageAccessor.get(result1.getId());
        Resource ap1Resource = resourceManager.getResource(FileResolver.resolveResourceId(ap1.getAutomationPackageResource()));
        ResourceRevisionFileHandle ap1File = resourceManager.getResourceFile(ap1Resource.getId().toHexString());
        Assert.assertNotNull(ap1File);

        Resource kwLibResource = resourceManager.getResource(FileResolver.resolveResourceId(ap1.getAutomationPackageLibraryResource()));
        ResourceRevisionFileHandle kwLibFile = resourceManager.getResourceFile(kwLibResource.getId().toHexString());
        Assert.assertNotNull(kwLibFile);

        // upload main AP (by resource id) + SNAPSHOT LIB (by resource id) - VERSION 2
        AutomationPackageUpdateResult result2 = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withResourceId(ap1Resource.getId().toHexString()),
                AutomationPackageFileSource.withResourceId(kwLibResource.getId().toHexString()),
                "v2", null, null, o -> true, o -> true, false,
                "testUser", true,
                true);

        // AP reuses old resource, but have new ID
        AutomationPackage ap2 = automationPackageAccessor.get(result2.getId());
        Assert.assertNotEquals(ap1.getId(), ap2.getId());

        // the resources have been reused
        Assert.assertEquals(ap1.getAutomationPackageResource(), ap2.getAutomationPackageResource());
        Assert.assertEquals(ap1.getAutomationPackageLibraryResource(), ap2.getAutomationPackageLibraryResource());

        checkResourceCleanup(ap1Resource.getId().toHexString(), ap1File, kwLibResource.getId().toHexString(), kwLibFile);
    }

    @Test
    public void testCreateAutomationPackageByResourceId() {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File kwLibSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);

        Resource savedApResource;
        Resource savedkwResource;
        try (InputStream is = new FileInputStream(automationPackageJar);
             InputStream kwIs = new FileInputStream(kwLibSnapshotJar);) {
            savedApResource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_AP, is, SAMPLE1_FILE_NAME, null, "testUser");
            savedkwResource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_AP_LIBRARY, kwIs, KW_LIB_FILE_NAME, null, "testUser");
        } catch (IOException | InvalidResourceFormatException e) {
            throw new RuntimeException("Unexpected exception", e);
        }

        // upload main AP (sample SNAPSHOT) + SNAPSHOT LIB - VERSION 1
        AutomationPackageUpdateResult result1 = manager.createOrUpdateAutomationPackage(
                true, true, null,
                AutomationPackageFileSource.withResourceId(savedApResource.getId().toHexString()),
                AutomationPackageFileSource.withResourceId(savedkwResource.getId().toHexString()),
                "v1", null, null, o -> true, o -> true, false,
                "testUser", true,
                true);

        AutomationPackage ap1 = automationPackageAccessor.get(result1.getId());

        // the resources have been reused
        Assert.assertEquals(savedApResource.getId().toHexString(), FileResolver.resolveResourceId(ap1.getAutomationPackageResource()));
        Assert.assertEquals(savedkwResource.getId().toHexString(), FileResolver.resolveResourceId(ap1.getAutomationPackageLibraryResource()));

        ResourceRevisionFileHandle apFile = resourceManager.getResourceFile(savedApResource.getId().toHexString());
        ResourceRevisionFileHandle kwLibFile = resourceManager.getResourceFile(savedkwResource.getId().toHexString());

        checkResourceCleanup(savedApResource.getId().toHexString(), apFile, savedkwResource.getId().toHexString(), kwLibFile);
    }

    private void checkResourceCleanup(String apResourceId, ResourceRevisionFileHandle ap1File,
                                      String kwLibResourceId, ResourceRevisionFileHandle kwLibFile) {
        // check that all used can be deleted (not blocked)
        log.info("Delete AP resource: {}", apResourceId);
        resourceManager.deleteResource(apResourceId);
        Assert.assertFalse(ap1File.getResourceFile().exists());

        if (kwLibResourceId != null) {
            log.info("Delete keyword resource: {}", kwLibResourceId);
            resourceManager.deleteResource(kwLibResourceId);
            Assert.assertFalse(kwLibFile.getResourceFile().exists());
        }
    }

    private void checkResources(AutomationPackage ap1, String expectedApFileName, String expectedKwFileName,
                                String expectedApOrigin, String expectedKwOrigin) {
        Resource ap1Resource = resourceManager.getResource(FileResolver.resolveResourceId(ap1.getAutomationPackageResource()));
        Resource kwLibResource = resourceManager.getResource(FileResolver.resolveResourceId(ap1.getAutomationPackageLibraryResource()));
        Assert.assertEquals(expectedApFileName, resourceManager.getResourceFile(ap1Resource.getId().toHexString()).getResourceFile().getName());
        Assert.assertEquals(expectedKwFileName, resourceManager.getResourceFile(kwLibResource.getId().toHexString()).getResourceFile().getName());

        Assert.assertEquals(expectedApOrigin, ap1Resource.getOrigin());
        Assert.assertEquals(expectedKwOrigin, kwLibResource.getOrigin());
    }

    private void checkUploadedResource(DynamicValue<String> fileResourceReference, String expectedFileName) {
        FileResolver fileResolver = new FileResolver(resourceManager);
        String resourceReferenceString = fileResourceReference.get();
        Assert.assertTrue(resourceReferenceString.startsWith(FileResolver.RESOURCE_PREFIX));
        String resourceId = FileResolver.resolveResourceId(resourceReferenceString);
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
            result = manager.createAutomationPackage(sample1FileSource, null, null, null, "testUser", false, true, null, o -> true, o -> true);
        } else {
            AutomationPackageUpdateResult updateResult = manager.createOrUpdateAutomationPackage(true, true, null,
                    sample1FileSource,
                    null, null, null, null, o -> true, o -> true, async,
                    "testUser", false, true);
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
        if(sample1FileSource.getMode() == AutomationPackageFileSource.Mode.MAVEN){
            Assert.assertEquals(
                    sample1FileSource.getMavenArtifactIdentifier().toStringRepresentation(),
                    resourceByAutomationPackage.getOrigin()
            );
        } else if(sample1FileSource.getMode() == AutomationPackageFileSource.Mode.INPUT_STREAM){
            Assert.assertEquals("uploaded:", resourceByAutomationPackage.getOrigin());
        }

        // we don't add the link from resource to the automation package, because we need to support one-to-many relationship between resource and AP
        Assert.assertNull(r.storedPackage.getId().toString(), resourceByAutomationPackage.getCustomField("automationPackageId"));

        // upload package without keyword library
        Assert.assertNull(r.storedPackage.getAutomationPackageLibraryResource());

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
        Function kwRouteToController = findFunctionByClassAndName(r.storedFunctions, GeneralScriptFunction.class, ANNOTATED_KEYWORD_ROUTING_TO_CTRL);
            assertTrue(kwRouteToController.isExecuteLocally());
            Function kwRoutingCriteria = findFunctionByClassAndName(r.storedFunctions, GeneralScriptFunction.class, ANNOTATED_KEYWORD_ROUTING_CRITERIA);
            Map<String, String> expectedRoutingCriteria = Map.of("OS", "WINDOWS", "TYPE", "PLAYWRIGHT");
            assertEquals(expectedRoutingCriteria, kwRoutingCriteria.getTokenSelectionCriteria());findFunctionByClassAndName(r.storedFunctions, GeneralScriptFunction.class, INLINE_PLAN);
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