package step.automation.packages;

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import step.automation.packages.accessor.AutomationPackageAccessorImpl;
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

        AbstractFunctionType jMeterFunctionType = new JMeterFunctionType(new Configuration());
        Mockito.when(functionTypeRegistry.getFunctionTypeByFunction(Mockito.any())).thenAnswer(invocationOnMock -> {
            Object function = invocationOnMock.getArgument(0);
            if(function instanceof JMeterFunction){
                return jMeterFunctionType;
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
                executionScheduler
        );
    }

    @Test
    public void testCrud() throws IOException, SetupFunctionException, FunctionTypeException {
        // 1. Upload new package
        SampleUploadingResult r = uploadSample1WithAsserts(true);

        // 2. Update the package - some entities are updated, some entities are added
        String fileName = "step-automation-packages-sample1-extended.jar";
        File automationPackageJar = new File("src/test/resources/" + fileName);
        try (InputStream is = new FileInputStream(automationPackageJar)) {
            String result = manager.updateAutomationPackage(is, fileName, null);

            // id of existing package is returned
            Assert.assertEquals(r.storedPackage.getId().toString(), result);

            r.storedPackage = automationPackageAccessor.get(result);
            Assert.assertEquals("My package", r.storedPackage.getAttribute(AbstractOrganizableObject.NAME));

            // 1 plan has been updated, 1 plan has been added
            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(2, storedPlans.size());

            Plan updatedPlan = storedPlans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan")).findFirst().orElse(null);
            Assert.assertNotNull(updatedPlan);
            Assert.assertEquals(r.storedPlan.getId(), updatedPlan.getId());

            Assert.assertNotNull(storedPlans.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Test Plan 2")).findFirst().orElse(null));

            // 1 function has been updated, 1 function has been added
            List<Function> storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(2, storedFunctions.size());

            Function updatedFunction = storedFunctions.stream().filter(f -> f.getAttribute(AbstractOrganizableObject.NAME).equals("JMeter keyword from automation package")).findFirst().orElse(null);
            Assert.assertNotNull(updatedFunction);
            Assert.assertEquals(r.storedFunction.getId(), updatedFunction.getId());

            Assert.assertNotNull(storedFunctions.stream().filter(p -> p.getAttribute(AbstractOrganizableObject.NAME).equals("Another JMeter keyword from automation package")).findFirst().orElse(null));

            // 1 task has been updated, 1 task has been added
            List<ExecutiontTaskParameters> storedTasks = executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(2, storedTasks.size());

            ExecutiontTaskParameters updatedTask = storedTasks.stream().filter(t -> t.getAttribute(AbstractOrganizableObject.NAME).equals("firstSchedule")).findFirst().orElse(null);
            Assert.assertNotNull(updatedTask);
            Assert.assertEquals(r.storedTask.getId(), updatedTask.getId());
            Assert.assertEquals(r.storedPlan.getId(), updatedTask.getExecutionsParameters().getPlan().getId());

            Assert.assertNotNull(storedTasks.stream().filter(t -> t.getAttribute(AbstractOrganizableObject.NAME).equals("secondSchedule")).findFirst().orElse(null));
        }

        // 3. Upload the original sample again - added plans/functions/tasks from step 2 should be removed
        SampleUploadingResult r2 = uploadSample1WithAsserts(false);
        Assert.assertEquals(r.storedPackage.getId(), r2.storedPackage.getId());
        Assert.assertEquals(r.storedPlan.getId(), r2.storedPlan.getId());
        Assert.assertEquals(r.storedFunction.getId(), r2.storedFunction.getId());
        Assert.assertEquals(r.storedTask.getId(), r2.storedTask.getId());

        // 4. Delete package by name - everything should be removed
        manager.removeAutomationPackage("My package");

        Assert.assertEquals(0, automationPackageAccessor.stream().count());

        Map<String, String> packageIdCriteria = getAutomationPackageIdCriteria(r2.storedPackage.getId().toString());
        Assert.assertEquals(0, planAccessor.findManyByCriteria(packageIdCriteria).count());
        Assert.assertEquals(0, functionAccessor.findManyByCriteria(packageIdCriteria).count());
        Assert.assertEquals(0, executionTaskAccessor.findManyByCriteria(packageIdCriteria).count());
    }

    private SampleUploadingResult uploadSample1WithAsserts(boolean createNew) throws IOException, FunctionTypeException, SetupFunctionException {
        String fileName = "step-automation-packages-sample1.jar";
        File automationPackageJar = new File("src/test/resources/" + fileName);

        SampleUploadingResult r = new SampleUploadingResult();
        try (InputStream is = new FileInputStream(automationPackageJar)) {
            String result = createNew ? manager.createAutomationPackage(is, fileName, null) : manager.updateAutomationPackage(is, fileName, null);

            r.storedPackage = automationPackageAccessor.get(result);
            Assert.assertEquals("My package", r.storedPackage.getAttribute(AbstractOrganizableObject.NAME));

            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedPlans.size());

            r.storedPlan = storedPlans.get(0);
            Assert.assertEquals("Test Plan", r.storedPlan.getAttribute(AbstractOrganizableObject.NAME));

            List<Function> storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedFunctions.size());
            r.storedFunction = storedFunctions.get(0);
            Assert.assertEquals("JMeter keyword from automation package", r.storedFunction.getAttribute(AbstractOrganizableObject.NAME));

            List<ExecutiontTaskParameters> storedTasks = executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedTasks.size());
            r.storedTask = storedTasks.get(0);
            Assert.assertEquals("firstSchedule", r.storedTask.getAttribute(AbstractOrganizableObject.NAME));
            Assert.assertEquals("0 15 10 ? * *", r.storedTask.getCronExpression());
            Assert.assertEquals(r.storedPlan.getId(), r.storedTask.getExecutionsParameters().getPlan().getId());
        }
        return r;
    }

    private static Map<String, String> getAutomationPackageIdCriteria(String automationPackageId) {
        Map<String, String> criteria = new HashMap<>();
        criteria.put("customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID, automationPackageId);
        return criteria;
    }

    private static class SampleUploadingResult {
        private AutomationPackage storedPackage;
        private Plan storedPlan;
        private Function storedFunction;
        private ExecutiontTaskParameters storedTask;
    }
}