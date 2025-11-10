/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.automation.packages;

import ch.exense.commons.app.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.automation.packages.accessor.AutomationPackageAccessorImpl;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.scheduler.AutomationPackageSchedulerPlugin;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.Accessor;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.controller.ControllerSettingAccessorImpl;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;
import step.core.plans.PlanAccessorImpl;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessorImpl;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.Executor;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.accessor.FunctionAccessorImpl;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.MockedGridClientImpl;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plugins.functions.types.CompositeFunctionType;
import step.plugins.java.GeneralScriptFunctionType;
import step.plugins.jmeter.JMeterFunctionType;
import step.plugins.node.NodeFunctionType;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceRevisionFileHandle;

import java.util.HashMap;
import java.util.List;

import static step.automation.packages.AutomationPackageLocks.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT;

public abstract class AbstractAutomationPackageManagerTest {
    protected static final String SAMPLE1_FILE_NAME = "step-automation-packages-sample1.jar";
    protected static final String SAMPLE1_EXTENDED_FILE_NAME = "step-automation-packages-sample1-extended.jar";
    protected static final String SAMPLE_ECHO_FILE_NAME = "step-automation-packages-sample-echo.jar";
    protected static final String KW_LIB_CALL_FILE_NAME = "step-automation-packages-kw-lib-call.jar";
    protected static final String KW_LIB_FILE_NAME = "step-automation-packages-kw-lib.jar";
    protected static final String KW_LIB_FILE_UPDATED_NAME = "step-automation-packages-kw-lib-updated.jar";
    protected static final String KW_LIB_FILE_RELEASE_NAME = "step-automation-packages-kw-lib-release.jar";
    private static final Logger log = LoggerFactory.getLogger(AbstractAutomationPackageManagerTest.class);

    protected AutomationPackageManager manager;
    protected AutomationPackageAccessorImpl automationPackageAccessor;
    protected LocalResourceManagerImpl resourceManager;

    protected FunctionManagerImpl functionManager;
    protected FunctionAccessorImpl functionAccessor;
    protected PlanAccessorImpl planAccessor;
    protected ExecutionTaskAccessorImpl executionTaskAccessor;
    protected ExecutionScheduler executionScheduler;
    protected Accessor<Parameter> parameterAccessor;

    protected AutomationPackageLocks automationPackageLocks = new AutomationPackageLocks(AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT);

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

    @Before
    public void before() {

        this.automationPackageAccessor = new AutomationPackageAccessorImpl(new InMemoryCollection<>());
        this.functionAccessor = new FunctionAccessorImpl(new InMemoryCollection<>());
        this.parameterAccessor = new AbstractAccessor<>(new InMemoryCollection<>());
        ParameterManager parameterManager = new ParameterManager(this.parameterAccessor, null, "groovy", new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler())));

        Configuration configuration = AbstractAutomationPackageManagerTest.createTestConfiguration();
        this.resourceManager = new LocalResourceManagerImpl();
        FunctionTypeRegistry functionTypeRegistry = AbstractAutomationPackageManagerTest.prepareTestFunctionTypeRegistry(configuration, resourceManager);

        this.functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
        this.planAccessor = new PlanAccessorImpl(new InMemoryCollection<>());

        this.executionTaskAccessor = new ExecutionTaskAccessorImpl(new InMemoryCollection<>());

        // scheduler with mocked executor
        this.executionScheduler = new ExecutionScheduler(new ControllerSettingAccessorImpl(new InMemoryCollection<>()), executionTaskAccessor, Mockito.mock(Executor.class));
        AutomationPackageHookRegistry automationPackageHookRegistry = new AutomationPackageHookRegistry();
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageSchedulerPlugin.registerSchedulerHooks(automationPackageHookRegistry, serializationRegistry, executionScheduler);
        AutomationPackageParametersRegistration.registerParametersHooks(automationPackageHookRegistry, serializationRegistry, parameterManager);

        JavaAutomationPackageReader apReader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, automationPackageHookRegistry, serializationRegistry, configuration);
        AutomationPackageReaderRegistry automationPackageReaderRegistry = new AutomationPackageReaderRegistry(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, automationPackageHookRegistry, serializationRegistry);
        automationPackageReaderRegistry.register(apReader);

        this.manager = AutomationPackageManager.createMainAutomationPackageManager(
                automationPackageAccessor,
                functionManager,
                functionAccessor,
                planAccessor,
                resourceManager,
                automationPackageHookRegistry,
                automationPackageReaderRegistry,
                automationPackageLocks,
                null, -1,
                new ObjectHookRegistry()
        );

        this.manager.setProvidersResolver(new MockedAutomationPackageProvidersResolver(new HashMap<>(), resourceManager, automationPackageReaderRegistry));
    }

    @After
    public void after() {
        if (resourceManager != null) {
            resourceManager.cleanup();
        }
    }

    protected void checkResourceCleanup(String resourceId, ResourceRevisionFileHandle ap1File,
                                        String kwLibResourceId, ResourceRevisionFileHandle kwLibFile) {
        // check that all used can be deleted (not blocked)
        log.info("Delete AP resource: {}", resourceId);
        resourceManager.deleteResource(resourceId);
        Assert.assertFalse(ap1File.getResourceFile().exists());

        if (kwLibResourceId != null) {
            log.info("Delete keyword resource: {}", kwLibResourceId);
            resourceManager.deleteResource(kwLibResourceId);
            Assert.assertFalse(kwLibFile.getResourceFile().exists());
        }
    }

    protected static class SampleUploadingResult {
        protected AutomationPackage storedPackage;
        protected List<Plan> storedPlans;
        protected List<Function> storedFunctions;
        protected ExecutiontTaskParameters storedTask;
    }

}
