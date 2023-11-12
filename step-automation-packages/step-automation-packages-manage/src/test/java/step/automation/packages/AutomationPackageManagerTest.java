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
        String fileName = "step-automation-packages-sample1.jar";
        File automationPackageJar = new File("src/test/resources/" + fileName);
        Plan storedPlan;
        Function storedFunction;
        ExecutiontTaskParameters storedTask;
        try (InputStream is = new FileInputStream(automationPackageJar)) {
            String result = manager.createAutomationPackage(is, fileName, null);

            AutomationPackage storedPackage = automationPackageAccessor.get(result);
            Assert.assertEquals("My package", storedPackage.getAttribute(AbstractOrganizableObject.NAME));

            List<Plan> storedPlans = planAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedPlans.size());

            storedPlan = storedPlans.get(0);
            Assert.assertEquals("Test Plan", storedPlan.getAttribute(AbstractOrganizableObject.NAME));

            List<Function> storedFunctions = functionAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedFunctions.size());
            storedFunction = storedFunctions.get(0);
            Assert.assertEquals("JMeter keyword from automation package", storedFunction.getAttribute(AbstractOrganizableObject.NAME));

            List<ExecutiontTaskParameters> storedTasks = executionTaskAccessor.findManyByCriteria(getAutomationPackageIdCriteria(result)).collect(Collectors.toList());
            Assert.assertEquals(1, storedTasks.size());
            storedTask = storedTasks.get(0);
            Assert.assertEquals("firstSchedule", storedTask.getAttribute(AbstractOrganizableObject.NAME));
            Assert.assertEquals("0 15 10 ? * *", storedTask.getCronExpression());
            Assert.assertEquals(storedPlan.getId(), storedTask.getExecutionsParameters().getPlan().getId());
        }
    }

    private static Map<String, String> getAutomationPackageIdCriteria(String automationPackageId) {
        Map<String, String> criteria = new HashMap<>();
        criteria.put("customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID, automationPackageId);
        return criteria;
    }

}