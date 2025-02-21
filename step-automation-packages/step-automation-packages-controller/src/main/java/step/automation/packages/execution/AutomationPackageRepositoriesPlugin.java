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
package step.automation.packages.execution;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackagePlugin;
import step.core.GlobalContext;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.scheduler.SchedulerPlugin;
import step.core.scheduler.housekeeping.HousekeepingJobsManager;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;

import java.io.File;
import java.time.Duration;
import java.util.function.Supplier;

import static step.automation.packages.execution.RepositoryWithAutomationPackageSupport.CONFIGURATION_MAVEN_FOLDER;
import static step.automation.packages.execution.RepositoryWithAutomationPackageSupport.DEFAULT_MAVEN_FOLDER;

@Plugin(dependencies = {AutomationPackagePlugin.class, SchedulerPlugin.class})
public class AutomationPackageRepositoriesPlugin extends AbstractControllerPlugin {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageRepositoriesPlugin.class);
    public static final String CONFIG_KEY_ISOLATED_AP_EXECUTION_TIMEOUT = "plugins.automation-package.isolated.execution.timeout";
    private static final Integer DEFAULT_ISOLATED_AP_EXECUTION_TIMEOUT = 0;
    public static final String ISOLATED_AP_HOUSEKEEPING_ENABLED = "isolated_ap_housekeeping_enabled";
    public static final String ISOLATED_AP_HOUSEKEEPING_JOB_CRON = "isolated_ap_housekeeping_job_cron";
    public static final String ISOLATED_AP_HOUSEKEEPING_TTL = "isolated_ap_housekeeping_ttl";
    private ControllerSettingAccessor controllerSettingAccessor;

    @Override
    public void initializeData(GlobalContext context) throws Exception {
        super.initializeData(context);

        // settings
         controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
        createIsolatedApControllerSettingsIfNecessary(context);
    }

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        super.afterInitializeData(context);

        File localRepository = context.getConfiguration().getPropertyAsFile(CONFIGURATION_MAVEN_FOLDER, new File(DEFAULT_MAVEN_FOLDER));

        // repository for isolated execution
        IsolatedAutomationPackageRepository isolatedApRepository = new IsolatedAutomationPackageRepository(
                context.require(AutomationPackageManager.class),
                context.getResourceManager(),
                context.require(FunctionTypeRegistry.class),
                context.require(FunctionAccessor.class),
                () -> {
                    ControllerSetting setting = controllerSettingAccessor.getSettingByKey(ISOLATED_AP_HOUSEKEEPING_TTL);
                    return setting == null ? null : setting.getValue();
                },
                localRepository.toPath()
        );
        context.getRepositoryObjectManager().registerRepository(AutomationPackageExecutor.ISOLATED_AUTOMATION_PACKAGE, isolatedApRepository);
        context.put(IsolatedAutomationPackageRepository.class, isolatedApRepository);

        // repository for deployed automation packages
        LocalAutomationPackageRepository localApRepository = new LocalAutomationPackageRepository(
                context.require(AutomationPackageManager.class),
                context.require(FunctionTypeRegistry.class),
                context.require(FunctionAccessor.class)
        );
        context.getRepositoryObjectManager().registerRepository(AutomationPackageExecutor.LOCAL_AUTOMATION_PACKAGE, localApRepository);
        context.put(LocalAutomationPackageRepository.class, localApRepository);

        Integer isolatedExecutionTimeout = context.getConfiguration().getPropertyAsInteger(CONFIG_KEY_ISOLATED_AP_EXECUTION_TIMEOUT, DEFAULT_ISOLATED_AP_EXECUTION_TIMEOUT);
        // isolated ap executor
        AutomationPackageExecutor packageExecutor = new AutomationPackageExecutor(
                context.require(AutomationPackageManager.class),
                context.getScheduler(),
                context.require(ExecutionAccessor.class),
                context.getRepositoryObjectManager(),
                isolatedExecutionTimeout
        );
        context.put(AutomationPackageExecutor.class, packageExecutor);

        // register cleanup job
        HousekeepingJobsManager housekeepingJobsManager = context.require(HousekeepingJobsManager.class);
        housekeepingJobsManager.registerManagedJob(new HousekeepingJobsManager.ManagedHousekeepingJob() {
            @Override
            protected Class<? extends Job> getJobClass() {
                return CleanupApResourcesJob.class;
            }

            @Override
            protected Supplier<? extends Job> getJobSupplier() {
                return (Supplier<Job>) () -> new CleanupApResourcesJob(context.require(IsolatedAutomationPackageRepository.class), context.require(ControllerSettingAccessor.class));
            }

            @Override
            protected String getName() {
                return ISOLATED_AP_HOUSEKEEPING_JOB_CRON;
            }

            @Override
            protected TriggerKey getTriggerKey() {
                return new TriggerKey("IsolatedApHousekeepingDataTrigger", "Housekeeping");
            }

            @Override
            protected JobKey getJobKey() {
                return new JobKey("IsolatedApHousekeepingData", "Housekeeping");
            }
        });

    }

    protected void createIsolatedApControllerSettingsIfNecessary(GlobalContext context) {
        createSettingIfNotExisting(context, ISOLATED_AP_HOUSEKEEPING_ENABLED, "true");
        createSettingIfNotExisting(context, ISOLATED_AP_HOUSEKEEPING_TTL, Long.toString(Duration.ofDays(1).toMillis()));
        createSettingIfNotExisting(context, ISOLATED_AP_HOUSEKEEPING_JOB_CRON, "0 8 * * * ?");
    }

    private static final class CleanupApResourcesJob implements Job {

        private final IsolatedAutomationPackageRepository repository;
        private final ControllerSettingAccessor controllerSettingAccessor;

        public CleanupApResourcesJob(IsolatedAutomationPackageRepository repository, ControllerSettingAccessor controllerSettingAccessor) {
            this.repository = repository;
            this.controllerSettingAccessor = controllerSettingAccessor;
        }

        @Override
        public void execute(JobExecutionContext context) {
            if (controllerSettingAccessor.getSettingAsBoolean(ISOLATED_AP_HOUSEKEEPING_ENABLED)) {
                repository.cleanUpOutdatedResources();
                repository.cleanUpMavenCache();
            }
        }
    }

    protected void createSettingIfNotExisting(GlobalContext context, String key, String defaultValue) {
        ControllerSettingAccessor controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
        ControllerSetting housekeepingEnabled = controllerSettingAccessor.getSettingByKey(key);
        if (housekeepingEnabled == null) {
            log.info("Set default controller value: {}={}", key, defaultValue);
            controllerSettingAccessor.save(new ControllerSetting(key, defaultValue));
        }
    }
}
