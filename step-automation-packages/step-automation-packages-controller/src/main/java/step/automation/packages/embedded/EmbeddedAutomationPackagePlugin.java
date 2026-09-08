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
package step.automation.packages.embedded;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackagePlugin;
import step.automation.packages.migration.KeywordPackageMigrationPlugin;
import step.core.GlobalContext;
import step.core.objectenricher.AttributeResolverRegistry;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

/**
 * Imports the automation packages shipped with the distribution.
 *
 * <p>The dependency on {@link KeywordPackageMigrationPlugin} is required and not given by the
 * lifecycle, both running in {@code afterInitializeData}: the migration deletes the keyword packages
 * that came from the embedded folder together with their keywords
 */
@Plugin(dependencies = {AutomationPackagePlugin.class, KeywordPackageMigrationPlugin.class})
public class EmbeddedAutomationPackagePlugin extends AbstractControllerPlugin {

    public static final String CONFIGURATION_EMBEDDED_FOLDER = "automation.packages.embedded.folder";

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedAutomationPackagePlugin.class);

    @Override
    public void afterInitializeData(GlobalContext context) throws Exception {
        super.afterInitializeData(context);

        String packageFolder = context.getConfiguration().getProperty(CONFIGURATION_EMBEDDED_FOLDER);
        if (packageFolder == null) {
            logger.debug("No embedded automation package folder configured under {}.", CONFIGURATION_EMBEDDED_FOLDER);
        } else {
            new EmbeddedAutomationPackageImporter(
                    context.require(AutomationPackageManager.class),
                    context.get(ObjectHookRegistry.class),
                    context.require(AttributeResolverRegistry.class)
            ).importEmbeddedAutomationPackages(packageFolder);
        }
    }
}
