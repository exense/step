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
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        FunctionTypeRegistry functionTypeRegistry = prepareTestFunctionTypeRegistry(configuration);

        this.functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
        this.planAccessor = new PlanAccessorImpl(new InMemoryCollection<>());
        this.resourceManager = new LocalResourceManagerImpl();

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
    }

    private static FunctionTypeRegistry prepareTestFunctionTypeRegistry(Configuration configuration) {
        FunctionTypeRegistry functionTypeRegistry = Mockito.mock(FunctionTypeRegistry.class);

        AbstractFunctionType<?> jMeterFunctionType = new JMeterFunctionType(configuration);
        AbstractFunctionType<?> generalScriptFunctionType = new GeneralScriptFunctionType(configuration);
        AbstractFunctionType<?> compositeFunctionType = new CompositeFunctionType(new ObjectHookRegistry());
        AbstractFunctionType<?> nodeFunctionType = new NodeFunctionType();

        Mockito.when(functionTypeRegistry.getFunctionTypeByFunction(Mockito.any())).thenAnswer(invocationOnMock -> {
            Object function = invocationOnMock.getArgument(0);
            if (function instanceof JMeterFunction) {
                return jMeterFunctionType;
            } else if (function instanceof GeneralScriptFunction) {
                return generalScriptFunctionType;
            } else if (function instanceof CompositeFunction) {
                return compositeFunctionType;
            } else if (function instanceof NodeFunction) {
                return nodeFunctionType;
            } else {
                return null;
            }
        });
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
        SampleUploadingResult r = uploadSample1WithAsserts(true, false, false);

        // 2. Update the package - some entities are updated, some entities are added
        String fileName = "step-automation-packages-sample1-extended.jar";
        File automationPackageJar = new File("src/test/resources/samples/" + fileName);
        try (InputStream is = new FileInputStream(automationPackageJar)) {
            AutomationPackageUpdateResult result = manager.createOrUpdateAutomationPackage(true, true, null, is, fileName, null, null, null, null, null, false);
            Assert.assertEquals(AutomationPackageUpdateStatus.UPDATED, result.getStatus());
            ObjectId resultId = result.getId();

            // id of existing package is returned
            Assert.assertEquals(r.storedPackage.getId().toString(), resultId.toString());

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
        }

        // 3. Upload the original sample again - added plans/functions/tasks from step 2 should be removed
        SampleUploadingResult r2 = uploadSample1WithAsserts(false, false, false);
        Assert.assertEquals(r.storedPackage.getId(), r2.storedPackage.getId());
        Assert.assertEquals(findPlanByName(r.storedPlans, PLAN_NAME_FROM_DESCRIPTOR), findPlanByName(r2.storedPlans, PLAN_NAME_FROM_DESCRIPTOR));
        Assert.assertEquals(toIds(r.storedFunctions), toIds(r2.storedFunctions));
        Assert.assertEquals(r.storedTask.getId(), r2.storedTask.getId());

        // 4. Delete package by name - everything should be removed
        manager.removeAutomationPackage(r2.storedPackage.getId(), null);

        Assert.assertEquals(0, automationPackageAccessor.stream().count());

        Map<String, String> packageIdCriteria = getAutomationPackageIdCriteria(r2.storedPackage.getId());
        Assert.assertEquals(0, planAccessor.findManyByCriteria(packageIdCriteria).count());
        Assert.assertEquals(0, functionAccessor.findManyByCriteria(packageIdCriteria).count());
        Assert.assertEquals(0, executionTaskAccessor.findManyByCriteria(packageIdCriteria).count());
        Assert.assertEquals(0, parameterAccessor.findManyByCriteria(packageIdCriteria).count());
    }

    @Test
    public void testUpdateMetadata() throws IOException {
        // 1. Upload new package
        SampleUploadingResult r = uploadSample1WithAsserts(true, false, false);

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
            Assert.assertNull( storedFunction.getActivationExpression());
        }
    }

    @Test
    public void testResourcesInKeywordsAndPlans() throws IOException {
        String fileName = "step-automation-packages-sample2.jar";
        File automationPackageJar = new File("src/test/resources/samples/" + fileName);

        try (InputStream is = new FileInputStream(automationPackageJar)) {
            ObjectId result;
            result = manager.createAutomationPackage(is, fileName, null, null, null, null, null);
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
            manager.createAutomationPackage(is, "picture.png", null, null, null, null, null);
            Assert.fail("The exception should be thrown in case of invalid automation package file");
        } catch (AutomationPackageManagerException ex) {
            // ok - invalid file should cause the exception
        }
    }

    @Test
    public void testZipArchive() throws IOException {
        try (InputStream is = new FileInputStream("src/test/resources/step/automation/packages/step-automation-packages.zip")) {
            ObjectId result;
            result = manager.createAutomationPackage(is, "step-automation-packages.zip", null, null, null, null, null);
            AutomationPackage storedPackage = automationPackageAccessor.get(result);

            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(4, storedPlans.size());
        }
    }

    @Test
    public void testUpdateAsync() throws IOException, InterruptedException {
        // 1. Upload new package
        SampleUploadingResult r = uploadSample1WithAsserts(true, true, false);
        uploadSample1WithAsserts(false, true, false);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(() -> {
            PlanRunnerResult execute = newExecutionEngineBuilder().build().execute(r.storedPlans.get(0));
        });
        //Give some time to let the execution start
        Thread.sleep(500);
        uploadSample1WithAsserts(false, true, true);
    }

    @Test
    public void testGetAllEntities() throws IOException {
        // 1. Upload new package
        SampleUploadingResult r = uploadSample1WithAsserts(true, false, false);

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

    private SampleUploadingResult uploadSample1WithAsserts(boolean createNew, boolean async, boolean expectedDelay) throws IOException {
        String fileName = "step-automation-packages-sample1.jar";
        File automationPackageJar = new File("src/test/resources/samples/" + fileName);

        SampleUploadingResult r = new SampleUploadingResult();
        try (InputStream is = new FileInputStream(automationPackageJar)) {
            ObjectId result;
            if (createNew) {
                result = manager.createAutomationPackage(is, fileName, null, null, null,  null,null);
            } else {
                AutomationPackageUpdateResult updateResult = manager.createOrUpdateAutomationPackage(true, true, null, is, fileName, null, null, null, null, null, async);
                if (async && expectedDelay) {
                    Assert.assertEquals(AutomationPackageUpdateStatus.UPDATE_DELAYED, updateResult.getStatus());
                } else {
                    Assert.assertEquals(AutomationPackageUpdateStatus.UPDATED, updateResult.getStatus());
                }
                result = updateResult.getId();
            }

            r.storedPackage = automationPackageAccessor.get(result);
            Assert.assertEquals("My package", r.storedPackage.getAttribute(AbstractOrganizableObject.NAME));

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
        }
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