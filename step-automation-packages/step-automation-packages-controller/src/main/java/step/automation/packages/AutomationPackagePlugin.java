/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.automation.packages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.automation.packages.accessor.AutomationPackageAccessorImpl;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.execution.AutomationPackageExecutor;
import step.automation.packages.scheduler.AutomationPackageSchedulerPlugin;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.GlobalContext;
import step.core.agents.provisioning.driver.AgentProvisioningStatus;
import step.core.collections.Collection;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.objectenricher.ObjectPredicate;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.functions.accessor.FunctionAccessor;
import step.functions.manager.FunctionManager;
import step.functions.plugin.FunctionControllerPlugin;
import step.repositories.ArtifactRepositoryConstants;
import step.resources.ResourceManagerControllerPlugin;

import java.io.File;
import java.util.Map;

import static step.automation.packages.AutomationPackageLocks.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS;
import static step.automation.packages.AutomationPackageLocks.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT;
import static step.automation.packages.execution.RepositoryWithAutomationPackageSupport.CONFIGURATION_MAVEN_FOLDER;
import static step.automation.packages.execution.RepositoryWithAutomationPackageSupport.DEFAULT_MAVEN_FOLDER;
import static step.repositories.ArtifactRepositoryConstants.MAVEN_EMPTY_SETTINGS;

@Plugin(dependencies = {ObjectHookControllerPlugin.class, ResourceManagerControllerPlugin.class, FunctionControllerPlugin.class, AutomationPackageSchedulerPlugin.class, ControllerSettingPlugin.class})
public class AutomationPackagePlugin extends AbstractControllerPlugin {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackagePlugin.class);
    protected AutomationPackageLocks automationPackageLocks;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        //Might be used by EE plugin creating AP manager EE
        Integer readLockTimeout = context.getConfiguration().getPropertyAsInteger(AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS,
                AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT);
        automationPackageLocks = new AutomationPackageLocks(readLockTimeout);
        context.put(AutomationPackageLocks.class, automationPackageLocks);

        Collection<AutomationPackage> collection = context.getCollectionFactory().getCollection(AutomationPackageEntity.entityName, AutomationPackage.class);

        AutomationPackageAccessor packageAccessor = new AutomationPackageAccessorImpl(collection);
        context.put(AutomationPackageAccessor.class, packageAccessor);
        context.getEntityManager().register(new AutomationPackageEntity(packageAccessor));

        Table<AutomationPackage> table = new Table<>(collection, "automation-package-read", true)
                .withResultItemTransformer(new AutomationPackageTableTransformer(context.getResourceManager()));
        context.get(TableRegistry.class).register(AutomationPackageEntity.entityName, table);

        context.getServiceRegistrationCallback().registerService(AutomationPackageServices.class);

        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();
        context.put(AutomationPackageHookRegistry.class, hookRegistry);

        AutomationPackageSerializationRegistry serRegistry = new AutomationPackageSerializationRegistry();
        context.put(AutomationPackageSerializationRegistry.class, serRegistry);

        context.put(AutomationPackageReader.class, new AutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serRegistry, context.getConfiguration()));
    }

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        super.afterInitializeData(context);

        if (context.get(AutomationPackageManager.class) == null) {
            log.info("Using the OS implementation of automation package manager");

            AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider = new MavenConfigProviderImpl(
                    context.require(ControllerSettingAccessor.class),
                    context.getConfiguration().getPropertyAsFile(CONFIGURATION_MAVEN_FOLDER, new File(DEFAULT_MAVEN_FOLDER))
            );

            // moved to 'afterInitializeData' to have the schedule accessor in context
            // here we pass the step.automation.packages.AutomationPackageMavenConfig.ConfigProvider to resolve maven settings dynamically
            // when we upload (deploy) the automation package from artifactory (the maven configuration can be changed either
            // via Step Parameters or via Controller settings)
            AutomationPackageManager packageManager = AutomationPackageManager.createMainAutomationPackageManager(
                    context.require(AutomationPackageAccessor.class),
                    context.require(FunctionManager.class),
                    context.require(FunctionAccessor.class),
                    context.getPlanAccessor(),
                    context.getResourceManager(),
                    context.require(AutomationPackageHookRegistry.class),
                    context.require(AutomationPackageReader.class),
                    automationPackageLocks,
                    mavenConfigProvider
            );
            context.put(AutomationPackageManager.class, packageManager);
        }
    }

    @Override
    public void serverStop(GlobalContext context) {
        super.serverStop(context);
        try {
            AutomationPackageManager automationPackageManager = context.get(AutomationPackageManager.class);
            if (automationPackageManager != null) {
                automationPackageManager.cleanup();
            }
        } catch (Exception e) {
            log.warn("Unable to finalize automaton package manager");
        }

        try {
            AutomationPackageExecutor executor = context.get(AutomationPackageExecutor.class);
            if (executor != null) {
                executor.shutdown();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted", e);
        }
    }

    @Override
    public ExecutionEnginePlugin getExecutionEnginePlugin() {
        return new AutomationPackageExecutionPlugin(automationPackageLocks);
    }

    private static class MavenConfigProviderImpl implements AutomationPackageMavenConfig.ConfigProvider {

        private final ControllerSettingAccessor controllerSettingAccessor;
        private final File localFileRepository;

        public MavenConfigProviderImpl(ControllerSettingAccessor controllerSettingAccessor, File localFileRepository) {
            this.controllerSettingAccessor = controllerSettingAccessor;
            this.localFileRepository = localFileRepository;
        }

        @Override
        public AutomationPackageMavenConfig getConfig(ObjectPredicate objectPredicate) {
            // default maven configuration in controller settings
            String mavenSettings;
            String settingsXml;

            // TODO: we indented to apply the user-defined multitenant parameter, but old Step Parameters are only designed for executions, so it will be replaced with special user settings (SED-3921)
            // the maven settings are used to deploy the automation package, so there no execution engine and bindings here
//            Map<String, Parameter> allParameters = parameterManager.getAllParameters(new HashMap<>(), objectPredicate);

            // here we take the name of maven settings property alike we do this for repository parameters in MavenArtifactRepository
            // but here we take this name from Step Parameters, but not from the execution context
//            Parameter mavenSettingsStepParameter = allParameters.get(ARTIFACT_PARAM_MAVEN_SETTINGS);
//            if (mavenSettingsStepParameter != null && mavenSettingsStepParameter.getValue().getValue() != null && !mavenSettingsStepParameter.getValue().getValue().isEmpty()) {
//                settingsXml = mavenSettingsStepParameter.getValue().getValue();
//            } else {
                // default maven configuration in controller settings
                mavenSettings = ArtifactRepositoryConstants.MAVEN_SETTINGS_PREFIX + ArtifactRepositoryConstants.ARTIFACT_PARAM_MAVEN_SETTINGS_DEFAULT;
                ControllerSetting controllerSetting = controllerSettingAccessor.getSettingByKey(mavenSettings);
//            }

            if (controllerSetting == null || controllerSetting.getValue() == null) {
                log.warn("No settings found for \"" + mavenSettings + "\", using empty settings instead.");
                controllerSettingAccessor.updateOrCreateSetting(mavenSettings, MAVEN_EMPTY_SETTINGS);
                controllerSetting = controllerSettingAccessor.getSettingByKey(mavenSettings);
                settingsXml = controllerSetting == null ? null : controllerSetting.getValue();
            } else {
                settingsXml = controllerSetting.getValue();
            }

            return new AutomationPackageMavenConfig(settingsXml, localFileRepository);
        }
    }
}
