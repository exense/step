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

import org.junit.Test;
import step.automation.packages.migration.KeywordPackageMigrationPlugin;
import step.core.plugins.ControllerPlugin;
import step.core.plugins.PluginManager;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class EmbeddedAutomationPackagePluginTest {

    /**
     * Both plugins act in {@code afterInitializeData}, so only the declared dependency keeps the
     * migration — which deletes the keyword packages that came from the embedded folder together with
     * their keywords — ahead of the deployment that replaces them. The wrong order fails on duplicate
     * keyword names, which is why it is asserted rather than left to the declaration.
     */
    @Test
    public void theEmbeddedImportRunsAfterTheKeywordPackageMigration() throws Exception {
        List<ControllerPlugin> sorted = PluginManager.builder(ControllerPlugin.class)
                .withPlugin(new EmbeddedAutomationPackagePlugin())
                .withPlugin(new KeywordPackageMigrationPlugin())
                .build()
                .getPlugins();

        assertTrue("the embedded automation packages must be imported after the keyword package migration",
                indexOf(sorted, KeywordPackageMigrationPlugin.class)
                        < indexOf(sorted, EmbeddedAutomationPackagePlugin.class));
    }

    private int indexOf(List<ControllerPlugin> plugins, Class<?> pluginClass) {
        for (int i = 0; i < plugins.size(); i++) {
            if (pluginClass.equals(plugins.get(i).getClass())) {
                return i;
            }
        }
        throw new AssertionError(pluginClass.getSimpleName() + " is not part of the sorted plugins");
    }
}
