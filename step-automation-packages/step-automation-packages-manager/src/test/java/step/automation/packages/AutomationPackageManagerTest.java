package step.automation.packages;

import ch.exense.commons.app.Configuration;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import step.automation.packages.accessor.AutomationPackageAccessorImpl;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.controller.ControllerSettingAccessorImpl;
import step.core.plans.Plan;
import step.core.plans.PlanAccessorImpl;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessorImpl;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.Executor;
import step.functions.Function;
import step.functions.accessor.FunctionAccessorImpl;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.SetupFunctionException;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.GeneralScriptFunctionType;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.jmeter.JMeterFunctionType;
import step.resources.LocalResourceManagerImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static step.automation.packages.AutomationPackageTestUtils.*;

public class AutomationPackageManagerTest {

    private AutomationPackageManager manager;
    private AutomationPackageAccessorImpl automationPackageAccessor;
    private FunctionManagerImpl functionManager;
    private FunctionAccessorImpl functionAccessor;
    private PlanAccessorImpl planAccessor;
    private LocalResourceManagerImpl resourceManager;
    private ExecutionTaskAccessorImpl executionTaskAccessor;
    private ExecutionScheduler executionScheduler;

    @Before
    public void before() {
        this.automationPackageAccessor = new AutomationPackageAccessorImpl(new InMemoryCollection<>());
        this.functionAccessor = new FunctionAccessorImpl(new InMemoryCollection<>());

        FunctionTypeRegistry functionTypeRegistry = Mockito.mock(FunctionTypeRegistry.class);

        Configuration configuration = new Configuration();
        AbstractFunctionType<?> jMeterFunctionType = new JMeterFunctionType(configuration);
        AbstractFunctionType<?> generalScriptFunctionType = new GeneralScriptFunctionType(configuration);

        Mockito.when(functionTypeRegistry.getFunctionTypeByFunction(Mockito.any())).thenAnswer(invocationOnMock -> {
            Object function = invocationOnMock.getArgument(0);
            if (function instanceof JMeterFunction) {
                return jMeterFunctionType;
            } else if (function instanceof GeneralScriptFunction) {
                return generalScriptFunctionType;
            } else {
                return null;
            }
        });

        this.functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
        this.planAccessor = new PlanAccessorImpl(new InMemoryCollection<>());
        this.resourceManager = new LocalResourceManagerImpl();

        this.executionTaskAccessor = new ExecutionTaskAccessorImpl(new InMemoryCollection<>());

        // scheduler with mocked executor
        this.executionScheduler = new ExecutionScheduler(new ControllerSettingAccessorImpl(new InMemoryCollection<>()), executionTaskAccessor, Mockito.mock(Executor.class));

        this.manager = new AutomationPackageManager(
                automationPackageAccessor,
                functionManager,
                functionAccessor,
                planAccessor,
                resourceManager,
                executionTaskAccessor,
                executionScheduler,
                null,
                YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH
        );
    }

    @Test
    public void testCrud() throws IOException, SetupFunctionException, FunctionTypeException {
        // 1. Upload new package
        SampleUploadingResult r = uploadSample1WithAsserts(true);

        // 2. Update the package - some entities are updated, some entities are added
        String fileName = "samples/step-automation-packages-sample1-extended.jar";
        File automationPackageJar = new File("src/test/resources/" + fileName);
        try (InputStream is = new FileInputStream(automationPackageJar)) {
            AutomationPackageManager.PackageUpdateResult result = manager.createOrUpdateAutomationPackage(true, true, null, is, fileName, null, null);
            Assert.assertEquals(AutomationPackageManager.PackageUpdateStatus.UPDATED, result.getStatus());
            ObjectId resultId = result.getId();

            // id of existing package is returned
            Assert.assertEquals(r.storedPackage.getId().toString(), resultId.toString());

            r.storedPackage = automationPackageAccessor.get(resultId);
            Assert.assertEquals("My package", r.storedPackage.getAttribute(AbstractOrganizableObject.NAME));

            // 3 plans have been updated, 1 plan has been added
            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(resultId)).collect(Collectors.toList());
            Assert.assertEquals(4, storedPlans.size());

            Plan updatedPlan = storedPlans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(PLAN_NAME_FROM_DESCRIPTOR)).findFirst().orElse(null);
            Assert.assertNotNull(updatedPlan);
            Assert.assertEquals(findPlanByName(r.storedPlans, PLAN_NAME_FROM_DESCRIPTOR).getId(), updatedPlan.getId());

            Assert.assertNotNull(storedPlans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals(PLAN_NAME_FROM_DESCRIPTOR_2)).findFirst().orElse(null));

            // 3 functions have been updated, 1 function has been added
            List<Function> storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(resultId)).collect(Collectors.toList());
            Assert.assertEquals(4, storedFunctions.size());

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
            Assert.assertEquals(findPlanByName(r.storedPlans, PLAN_NAME_FROM_DESCRIPTOR).getId(), updatedTask.getExecutionsParameters().getPlan().getId());

            // new task is configured as inactive in sample
            ExecutiontTaskParameters newTask = storedTasks.stream().filter(t -> t.getAttribute(AbstractOrganizableObject.NAME).equals(SCHEDULE_2)).findFirst().orElse(null);
            Assert.assertNotNull(newTask);
            Assert.assertFalse(newTask.isActive());
        }

        // 3. Upload the original sample again - added plans/functions/tasks from step 2 should be removed
        SampleUploadingResult r2 = uploadSample1WithAsserts(false);
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
    }

    private SampleUploadingResult uploadSample1WithAsserts(boolean createNew) throws IOException, FunctionTypeException, SetupFunctionException {
        String fileName = "samples/step-automation-packages-sample1.jar";
        File automationPackageJar = new File("src/test/resources/" + fileName);

        SampleUploadingResult r = new SampleUploadingResult();
        try (InputStream is = new FileInputStream(automationPackageJar)) {
            ObjectId result;
            if (createNew) {
                result = manager.createAutomationPackage(is, fileName, null, null);
            } else {
                AutomationPackageManager.PackageUpdateResult updateResult = manager.createOrUpdateAutomationPackage(true, true, null, is, fileName, null, null);
                Assert.assertEquals(AutomationPackageManager.PackageUpdateStatus.UPDATED, updateResult.getStatus());
                result = updateResult.getId();
            }

            r.storedPackage = automationPackageAccessor.get(result);
            Assert.assertEquals("My package", r.storedPackage.getAttribute(AbstractOrganizableObject.NAME));

            // 2 annotated plans and 1 plan from yaml descriptor
            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(3, storedPlans.size());

            r.storedPlans = storedPlans;
            Plan planFromDescriptor = findPlanByName(storedPlans, PLAN_NAME_FROM_DESCRIPTOR);
            Assert.assertNotNull(planFromDescriptor);
            Assert.assertNotNull(findPlanByName(storedPlans, PLAN_FROM_PLANS_ANNOTATION));
            Assert.assertNotNull(findPlanByName(storedPlans, INLINE_PLAN));

            r.storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(3, r.storedFunctions.size());
            findFunctionByClassAndName(r.storedFunctions, JMeterFunction.class, J_METER_KEYWORD_1);
            findFunctionByClassAndName(r.storedFunctions, GeneralScriptFunction.class, ANNOTATED_KEYWORD);
            findFunctionByClassAndName(r.storedFunctions, GeneralScriptFunction.class, INLINE_PLAN);

            List<ExecutiontTaskParameters> storedTasks = executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedTasks.size());
            r.storedTask = storedTasks.get(0);
            Assert.assertEquals(SCHEDULE_1, r.storedTask.getAttribute(AbstractOrganizableObject.NAME));
            Assert.assertEquals("0 15 10 ? * *", r.storedTask.getCronExpression());
            Assert.assertTrue(r.storedTask.isActive());
            Assert.assertEquals(planFromDescriptor.getId(), r.storedTask.getExecutionsParameters().getPlan().getId());
        }
        return r;
    }

    private static Map<String, String> getAutomationPackageIdCriteria(ObjectId automationPackageId) {
        Map<String, String> criteria = new HashMap<>();
        criteria.put("customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID, automationPackageId.toString());
        return criteria;
    }

    private static class SampleUploadingResult {
        private AutomationPackage storedPackage;
        private List<Plan> storedPlans;
        private List<Function> storedFunctions;
        private ExecutiontTaskParameters storedTask;
    }
}