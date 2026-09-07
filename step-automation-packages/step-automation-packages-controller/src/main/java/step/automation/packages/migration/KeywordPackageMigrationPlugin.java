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
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.core.objectenricher.ObjectHookRegistry;
import step.migration.MigrationManager;
import step.migration.MigrationManagerPlugin;
import step.functions.accessor.FunctionAccessor;

/**
 * Migrate the keyword packages using the provided mode per configuration
 * <ul>
 *     <li>Migrate: migrate to automation packages</li>
 *     <li>Detach: delete the package entity and unlink the deployed keywords</li>
 *     <li>Delete: delete the package entity, its keywords and Step resources</li>
 * </ul>
 *
 * <p>The migration task itself only remove the functionPackage entities (the collection is renamed),
 * since the migration tasks are also used at import of Step exported archive for which the keywords
 * and resources must be kept.
 * </p>
 * <p>The 2nd phase is executed in the afterInitializeData of the controller plugin using the Resource and
 * Automation Package managers
 *</p>
 * <p>Because it drains the staging collection on every startup rather than once, an interruption
 * between the two phases is picked up on the next boot even though the migration task itself will not
 * run again.
 * </p>
 */
@Plugin(dependencies = {MigrationManagerPlugin.class, AutomationPackagePlugin.class})
public class KeywordPackageMigrationPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(KeywordPackageMigrationPlugin.class);

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        context.require(MigrationManager.class).register(KeywordPackageMigrationTask.class);
    }

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        KeywordPackageMigrationMode mode = resolveMode(context);

        new KeywordPackageMigrationExecutor(
                context.getCollectionFactory().getCollection(
                        KeywordPackageMigrationTask.STAGING_COLLECTION, StagedKeywordPackage.class),
                context.require(FunctionAccessor.class),
                context.require(AutomationPackageAccessor.class),
                context.require(AutomationPackageManager.class),
                context.getResourceManager(),
                context.get(ObjectHookRegistry.class),
                mode
        ).run();
    }

    /**
     * A rejected value has to stop the startup rather than fall back to the default. Only a
     * {@link PluginCriticalException} propagates out of a plugin lifecycle method — anything else is
     * logged and swallowed — so a mistyped {@code detach} would otherwise boot straight into the mode
     * that rebuilds keywords and discards their manual reconfiguration.
     */
    private KeywordPackageMigrationMode resolveMode(GlobalContext context) {
        try {
            KeywordPackageMigrationMode mode = KeywordPackageMigrationMode.parse(
                    context.getConfiguration().getProperty(KeywordPackageMigrationMode.PROPERTY_KEY));
            logger.info("Keyword package migration mode: {}", mode.getPropertyValue());
            return mode;
        } catch (IllegalArgumentException e) {
            throw new PluginCriticalException(e.getMessage(), e);
        }
    }
}
