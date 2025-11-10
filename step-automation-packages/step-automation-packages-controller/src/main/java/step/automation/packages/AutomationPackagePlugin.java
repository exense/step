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
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.Collection;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.deployment.ObjectHookControllerPlugin;
import step.core.imports.ImportContext;
import step.core.objectenricher.ObjectHookRegistry;
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
import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;

import static step.automation.packages.AutomationPackageLocks.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS;
import static step.automation.packages.AutomationPackageLocks.AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT;
import static step.automation.packages.execution.RepositoryWithAutomationPackageSupport.CONFIGURATION_MAVEN_FOLDER;
import static step.automation.packages.execution.RepositoryWithAutomationPackageSupport.DEFAULT_MAVEN_FOLDER;
import static step.repositories.ArtifactRepositoryConstants.MAVEN_EMPTY_SETTINGS;

@Plugin(dependencies = {ObjectHookControllerPlugin.class, ResourceManagerControllerPlugin.class, FunctionControllerPlugin.class, AutomationPackageSchedulerPlugin.class, ControllerSettingPlugin.class})
public class AutomationPackagePlugin extends AbstractControllerPlugin {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackagePlugin.class);
    public static final String CONFIGURATION_MAVEN_MAX_AGE = "repository.artifact.maven.max.age.minutes";
    public static final String CONFIGURATION_MAVEN_CLEANUP_FREQUENCY = "repository.artifact.maven.cleanup.frequency.minutes";
    public static final Long DEFAULT_MAVEN_MAX_AGE = 1440L;
    public static final Long DEFAULT_MAVEN_CLEANUP_FREQUENCY = 60L;
    private static final Integer DEFAULT_MAX_VERSIONS_PER_AP = 0; //quota disabled
    private static final String CONFIGURATION_MAX_VERSIONS_PER_AP = "automation.packages.max.versions.per.package";
    protected AutomationPackageLocks automationPackageLocks;

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        super.serverStart(context);

        //Might be used by EE plugin creating AP manager EE
        Integer readLockTimeout = context.getConfiguration().getPropertyAsInteger(AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS,
                AUTOMATION_PACKAGE_READ_LOCK_TIMEOUT_SECS_DEFAULT);
        automationPackageLocks = new AutomationPackageLocks(readLockTimeout);
        context.put(AutomationPackageLocks.class, automationPackageLocks);

        // for table services we use the extended AutomationPackage object containing more information about linked resources
        Collection<AutomationPackageTableRecord> extendedCollection = context.getCollectionFactory().getCollection(AutomationPackageEntity.entityName, AutomationPackageTableRecord.class);
        Collection<AutomationPackage> collection = context.getCollectionFactory().getCollection(AutomationPackageEntity.entityName, AutomationPackage.class);

        AutomationPackageAccessor packageAccessor = new AutomationPackageAccessorImpl(collection);
        context.put(AutomationPackageAccessor.class, packageAccessor);
        context.getEntityManager().register(new AutomationPackageEntity(packageAccessor));
        context.getEntityManager().registerImportHook(new AutomationPackageImportHook());

        Table<AutomationPackageTableRecord> table = new Table<>(extendedCollection, "automation-package-read", true)
                .withResultItemTransformer(new AutomationPackageTableTransformer(context.getResourceManager()));
        context.get(TableRegistry.class).register(AutomationPackageEntity.entityName, table);

        context.getServiceRegistrationCallback().registerService(AutomationPackageServices.class);

        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();
        context.put(AutomationPackageHookRegistry.class, hookRegistry);

        AutomationPackageSerializationRegistry serRegistry = new AutomationPackageSerializationRegistry();
        context.put(AutomationPackageSerializationRegistry.class, serRegistry);

        AutomationPackageReaderRegistry automationPackageReaderRegistry = new AutomationPackageReaderRegistry(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serRegistry);
        JavaAutomationPackageReader javaAutomationPackageReader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serRegistry, context.getConfiguration());
        automationPackageReaderRegistry.register(javaAutomationPackageReader);
        context.put(AutomationPackageReaderRegistry.class, automationPackageReaderRegistry);
    }

    public static class AutomationPackageImportHook implements BiConsumer<Object, ImportContext> {

        @Override
        public void accept(Object o, ImportContext importContext) {
            if (o instanceof AbstractIdentifiableObject) {
                AbstractIdentifiableObject entity = (AbstractIdentifiableObject) o;
                Optional.ofNullable(entity.getCustomFields()).ifPresent(fields -> fields.remove(AutomationPackageEntity.AUTOMATION_PACKAGE_ID));
            }
        }
    }

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        super.afterInitializeData(context);

        if (context.get(AutomationPackageManager.class) == null) {
            log.info("Using the OS implementation of automation package manager");

            AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider = new MavenConfigProviderImpl(
                    context.require(ControllerSettingAccessor.class),
                    context.getConfiguration().getPropertyAsFile(CONFIGURATION_MAVEN_FOLDER, new File(DEFAULT_MAVEN_FOLDER)),
                    Duration.ofMinutes(context.getConfiguration().getPropertyAsLong(CONFIGURATION_MAVEN_MAX_AGE, DEFAULT_MAVEN_MAX_AGE)),
                    Duration.ofMinutes(context.getConfiguration().getPropertyAsLong(CONFIGURATION_MAVEN_CLEANUP_FREQUENCY, DEFAULT_MAVEN_CLEANUP_FREQUENCY))
            );

            //Get parallel max version
            Integer maxVersionPerPackage = context.getConfiguration().getPropertyAsInteger(CONFIGURATION_MAX_VERSIONS_PER_AP, DEFAULT_MAX_VERSIONS_PER_AP);

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
                    context.require(AutomationPackageReaderRegistry.class),
                    automationPackageLocks,
                    mavenConfigProvider,
                    maxVersionPerPackage,
                    context.get(ObjectHookRegistry.class)
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
        private final Duration maxAge;
        private final Duration cleanupFrequency;

        public MavenConfigProviderImpl(ControllerSettingAccessor controllerSettingAccessor, File localFileRepository, Duration maxAge, Duration cleanupFrequency) {
            this.controllerSettingAccessor = controllerSettingAccessor;
            this.localFileRepository = localFileRepository;
            this.maxAge = maxAge;
            this.cleanupFrequency = cleanupFrequency;
        }

        @Override
        public AutomationPackageMavenConfig getConfig() {
            // default maven configuration in controller settings
            String mavenSettings;
            String settingsXml;

            mavenSettings = ArtifactRepositoryConstants.MAVEN_SETTINGS_PREFIX + ArtifactRepositoryConstants.ARTIFACT_PARAM_MAVEN_SETTINGS_DEFAULT;
            ControllerSetting controllerSetting = controllerSettingAccessor.getSettingByKey(mavenSettings);
            if (controllerSetting == null || controllerSetting.getValue() == null) {
                log.warn("No settings found for \"" + mavenSettings + "\", using empty settings instead.");
                controllerSettingAccessor.updateOrCreateSetting(mavenSettings, MAVEN_EMPTY_SETTINGS);
                controllerSetting = controllerSettingAccessor.getSettingByKey(mavenSettings);
                settingsXml = controllerSetting == null ? null : controllerSetting.getValue();
            } else {
                settingsXml = controllerSetting.getValue();
            }

            return new AutomationPackageMavenConfig(settingsXml, localFileRepository, maxAge, cleanupFrequency);
        }
    }
}
