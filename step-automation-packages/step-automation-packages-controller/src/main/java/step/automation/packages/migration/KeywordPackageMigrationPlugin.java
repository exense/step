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
package step.automation.packages.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackagePlugin;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.core.GlobalContext;
import step.core.Version;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.core.objectenricher.ObjectHookRegistry;
import step.migration.MigrationManager;
import step.migration.MigrationManagerPlugin;
import step.functions.accessor.FunctionAccessor;
import step.versionmanager.VersionManager;

/**
 * Migrate the keyword packages using the provided mode per configuration
 * <ul>
 *     <li>Migrate: migrate to automation packages</li>
 *     <li>Detach: delete the package entity and unlink the deployed keywords</li>
 *     <li>Delete: delete the package entity, its keywords and Step resources</li>
 * </ul>
 *
 * <p>The migration task {@link KeywordPackageMigrationTask}itself only remove the functionPackage entities (the collection is renamed)
 * for 2 reasons:
 * <ul><li>migration task only perform migration at the model level while using actual ResourceManager and AutomationPackageManger is required for the full migration</li>
 * <li>the {@code ImportManager} apply the same migration tasks, in which case migration to AP or deletion doens't apply</li></ul>
 * </p>
 * <p>The 2nd phase of the migration run by this plugin is also only triggered on the first start after the upgrade. The logic
 * of the
 * </p>
 * <p>An incorrect configuration of the mode is detected early and interrupt the startup before the migration tasks. The
 * {@link #RESTART_MIGRATION_HINT} tells the administrator what to do to fix the issue.
 * </p>
 */
@Plugin(dependencies = {MigrationManagerPlugin.class, AutomationPackagePlugin.class})
public class KeywordPackageMigrationPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(KeywordPackageMigrationPlugin.class);

    /**
     * The start of this controller is recorded before the migration runs, so correcting the
     * configuration is not enough on its own to get another attempt.
     */
    static final String RESTART_MIGRATION_HINT = "The keyword package migration only runs on the "
            + "startup that upgrades to " + KeywordPackageMigrationTask.AS_OF_VERSION
            + ", and this start has already been recorded with that version. Correct the property and "
            + "delete the most recent record of the 'controllerlogs' collection before starting again, "
            + "otherwise the keyword packages are left untouched and their keywords keep running as "
            + "they are.";

    private boolean migrationRequired;
    private KeywordPackageMigrationMode mode;

    /**
     * The version of the previous start is read by the version manager in its {@code init}, which runs
     * before this, so whether the migration applies is already known here. The mode is therefore only
     * read on the startup that migrates, and a value left behind in the configuration afterward can no
     * longer stop a controller from starting.
     * <p>
     * Resolving it here rather than in {@link #afterInitializeData(GlobalContext)} also keeps a rejected
     * value from stopping the startup after the migration task has renamed the keyword package
     * collection, which would leave the staging collection behind with nothing left to drain it.
     */
    @Override
    public void serverStart(GlobalContext context) throws Exception {
        context.require(MigrationManager.class).register(KeywordPackageMigrationTask.class);
        migrationRequired = migrationRequired(context);
        if (migrationRequired) {
            mode = resolveMode(context);
        }
    }

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        if (migrationRequired) {
            logger.info("Migrating the keyword packages in {} mode.", mode.getPropertyValue());
            // The staging collection is only looked up here: getting it creates it on the collection
            // factories that need to, which is what this whole check exists to avoid on every start.
            new KeywordPackageMigrationExecutor(
                    context.getCollectionFactory().getCollection(
                            KeywordPackageMigrationTask.STAGING_COLLECTION, StagedKeywordPackage.class),
                    context.require(FunctionAccessor.class),
                    context.require(AutomationPackageAccessor.class),
                    context.require(AutomationPackageManager.class),
                    context.getResourceManager(),
                    context.require(ObjectHookRegistry.class),
                    mode
            ).run();
        }
    }

    /**
     * Asks the migration framework whether it runs {@link KeywordPackageMigrationTask} on this startup,
     * rather than restating the condition: an empty previous version is the first start against this
     * database, where nothing is migrated at all.
     *
     * @return true if the migration task runs during this startup and leaves a staging collection behind
     */
    private boolean migrationRequired(GlobalContext context) {
        VersionManager<?> versionManager = context.require(VersionManager.class);
        return versionManager.getPreviousVersion()
                .map(previous -> MigrationManager.isMigrationTaskInScope(
                        KeywordPackageMigrationTask.AS_OF_VERSION, previous, context.require(Version.class)))
                .orElse(false);
    }

    /**
     * A rejected value has to stop the startup rather than fall back to the default. Only a
     * {@link PluginCriticalException} propagates out of a plugin lifecycle method — anything else is
     * logged and swallowed — so a mistyped {@code detach} would otherwise boot straight into the mode
     * that rebuilds keywords and discards their manual reconfiguration.
     */
    private KeywordPackageMigrationMode resolveMode(GlobalContext context) {
        try {
            return KeywordPackageMigrationMode.parse(
                    context.getConfiguration().getProperty(KeywordPackageMigrationMode.PROPERTY_KEY));
        } catch (IllegalArgumentException e) {
            throw new PluginCriticalException(e.getMessage() + ". " + RESTART_MIGRATION_HINT, e);
        }
    }
}
