package step.automation.packages;

import ch.exense.commons.app.Configuration;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
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
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;
import step.core.plans.PlanAccessorImpl;
import step.core.plans.runner.PlanRunnerResult;
import step.core.scheduler.*;
import step.datapool.excel.ExcelDataPool;
import step.engine.plugins.AbstractExecutionEnginePlugin;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static step.automation.packages.AutomationPackagePlugin.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT;
import static step.automation.packages.AutomationPackageTestUtils.*;

public class AutomationPackageManagerOSTest {

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
        ParameterManager parameterManager = new ParameterManager(this.parameterAccessor, null, "groovy");

        FunctionTypeRegistry functionTypeRegistry = Mockito.mock(FunctionTypeRegistry.class);

        Configuration configuration = new Configuration();
        AbstractFunctionType<?> jMeterFunctionType = new JMeterFunctionType(configuration);
        AbstractFunctionType<?> generalScriptFunctionType = new GeneralScriptFunctionType(configuration);
        AbstractFunctionType<?> compositeFunctionType = new CompositeFunctionType(new ObjectHookRegistry());

        Mockito.when(functionTypeRegistry.getFunctionTypeByFunction(Mockito.any())).thenAnswer(invocationOnMock -> {
            Object function = invocationOnMock.getArgument(0);
            if (function instanceof JMeterFunction) {
                return jMeterFunctionType;
            } else if (function instanceof GeneralScriptFunction) {
                return generalScriptFunctionType;
            } else if (function instanceof CompositeFunction){
                return compositeFunctionType;
            }  else {
                return null;
            }
        });

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
                new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, automationPackageHookRegistry, serializationRegistry),
                automationPackageLocks
                );
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
            AutomationPackageUpdateResult result = manager.createOrUpdateAutomationPackage(true, true, null, is, fileName, null, null, false);
            Assert.assertEquals(AutomationPackageUpdateStatus.UPDATED, result.getStatus());
            ObjectId resultId = result.getId();

            // id of existing package is returned
            Assert.assertEquals(r.storedPackage.getId().toString(), resultId.toString());

            r.storedPackage = automationPackageAccessor.get(resultId);
            Assert.assertEquals("My package", r.storedPackage.getAttribute(AbstractOrganizableObject.NAME));

            // 4 plans have been updated, 1 plan has been added
            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(resultId)).collect(Collectors.toList());
            Assert.assertEquals(5, storedPlans.size());

            Plan updatedPlan = storedPlans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(PLAN_NAME_FROM_DESCRIPTOR)).findFirst().orElse(null);
            Assert.assertNotNull(updatedPlan);
            Assert.assertEquals(findPlanByName(r.storedPlans, PLAN_NAME_FROM_DESCRIPTOR).getId(), updatedPlan.getId());

            Assert.assertNotNull(storedPlans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(PLAN_NAME_FROM_DESCRIPTOR_2)).findFirst().orElse(null));

            // 5 functions have been updated, 1 function has been added
            List<Function> storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(resultId)).collect(Collectors.toList());
            Assert.assertEquals(6, storedFunctions.size());

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
            Assert.assertFalse(newTask.isActive());

            // 1 parameter is saved
            List<Parameter> allParameters = parameterAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result.getId())).collect(Collectors.toList());
            Assert.assertEquals(1, allParameters.size());
            Parameter parameter = allParameters.get(0);
            assertEquals("myKey", parameter.getKey());
            assertEquals("myValue", parameter.getValue());
            assertEquals("some description", parameter.getDescription());
            assertEquals("abc", parameter.getActivationExpression().getScript());
            assertNull(parameter.getActivationExpression().getScriptEngine());
            assertEquals((Integer) 10, parameter.getPriority());
            assertEquals(true, parameter.getProtectedValue());
            assertEquals(ParameterScope.GLOBAL, parameter.getScope());
            assertEquals("entity", parameter.getScopeEntity());
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
    public void testResourcesInKeywordsAndPlans() throws IOException {
        String fileName = "step-automation-packages-sample2.jar";
        File automationPackageJar = new File("src/test/resources/samples/" + fileName);

        try (InputStream is = new FileInputStream(automationPackageJar)) {
            ObjectId result;
            result = manager.createAutomationPackage(is, fileName, null, null);
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
                result = manager.createAutomationPackage(is, fileName, null, null);
            } else {
                AutomationPackageUpdateResult updateResult = manager.createOrUpdateAutomationPackage(true, true, null, is, fileName, null, null, async);
                if (async && expectedDelay) {
                    Assert.assertEquals(AutomationPackageUpdateStatus.UPDATE_DELAYED, updateResult.getStatus());
                } else {
                    Assert.assertEquals(AutomationPackageUpdateStatus.UPDATED, updateResult.getStatus());
                }
                result = updateResult.getId();
            }

            r.storedPackage = automationPackageAccessor.get(result);
            Assert.assertEquals("My package", r.storedPackage.getAttribute(AbstractOrganizableObject.NAME));

            // 2 annotated plans and 2 plans from yaml descriptor
            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(4, storedPlans.size());

            r.storedPlans = storedPlans;
            Plan planFromDescriptor = findPlanByName(storedPlans, PLAN_NAME_FROM_DESCRIPTOR);
            Assert.assertNotNull(planFromDescriptor);
            Assert.assertNotNull(findPlanByName(storedPlans, PLAN_FROM_PLANS_ANNOTATION));
            Assert.assertNotNull(findPlanByName(storedPlans, INLINE_PLAN));
            Assert.assertNotNull(findPlanByName(storedPlans, PLAN_NAME_WITH_COMPOSITE));

            r.storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(5, r.storedFunctions.size());
            findFunctionByClassAndName(r.storedFunctions, JMeterFunction.class, J_METER_KEYWORD_1);
            findFunctionByClassAndName(r.storedFunctions, GeneralScriptFunction.class, ANNOTATED_KEYWORD);
            findFunctionByClassAndName(r.storedFunctions, GeneralScriptFunction.class, INLINE_PLAN);
            CompositeFunction compositeKeyword = (CompositeFunction) findFunctionByClassAndName(r.storedFunctions, CompositeFunction.class, COMPOSITE_KEYWORD);
            Assert.assertNotNull(compositeKeyword.getPlan());
            Assert.assertEquals(planFromDescriptor.getId(), compositeKeyword.getPlan().getId());

            List<ExecutiontTaskParameters> storedTasks = executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedTasks.size());
            r.storedTask = storedTasks.get(0);
            Assert.assertEquals(SCHEDULE_1, r.storedTask.getAttribute(AbstractOrganizableObject.NAME));
            Assert.assertEquals("0 15 10 ? * *", r.storedTask.getCronExpression());
            Assert.assertNotNull(r.storedTask.getCronExclusions());
            Assert.assertEquals(List.of("*/5 * * * * ?", "0 * * * * ?"), r.storedTask.getCronExclusions().stream().map(CronExclusion::getCronExpression).collect(Collectors.toList()));
            Assert.assertTrue(r.storedTask.isActive());
            Assert.assertEquals("local", r.storedTask.getExecutionsParameters().getRepositoryObject().getRepositoryID());
            Assert.assertEquals(planFromDescriptor.getId().toHexString(), r.storedTask.getExecutionsParameters().getRepositoryObject().getRepositoryParameters().get("planid"));

            List<Parameter> allParameters = parameterAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(2, allParameters.size());
            Parameter parameter = allParameters.get(0);
            assertEquals("myKey", parameter.getKey());
            assertEquals("myValue", parameter.getValue());
            assertEquals("some description", parameter.getDescription());
            assertEquals("abc", parameter.getActivationExpression().getScript());
            assertNull(parameter.getActivationExpression().getScriptEngine());
            assertEquals((Integer) 10, parameter.getPriority());
            assertEquals(true, parameter.getProtectedValue());
            assertEquals(ParameterScope.APPLICATION, parameter.getScope());
            assertEquals("entity", parameter.getScopeEntity());

            parameter = allParameters.get(1);
            assertEquals("mySimpleKey", parameter.getKey());
            assertEquals("mySimpleValue", parameter.getValue());
            assertEquals(ParameterScope.GLOBAL, parameter.getScope()); // global by default
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
                    public void beforeExecutionEnd(ExecutionContext context) {
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