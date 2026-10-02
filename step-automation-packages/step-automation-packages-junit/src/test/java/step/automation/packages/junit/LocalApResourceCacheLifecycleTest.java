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
package step.automation.packages.junit;

import org.junit.Test;
import step.core.execution.ExecutionEngine;
import step.core.execution.OperationMode;
import step.engine.plugins.LocalApResourceCacheRoot;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The {@code apResource:} materialisation cache of a local execution belongs to the engine, not to an
 * execution - an engine runs several and they share it - and it is deleted when that engine is closed,
 * which is what the runners and the CLI do at the end of a run.
 */
public class LocalApResourceCacheLifecycleTest {

    @Test
    public void theCacheDirectoryIsDeletedWithTheExecutionEngine() {
        File cacheRoot;
        try (ExecutionEngine executionEngine = ExecutionEngine.builder()
            .withOperationMode(OperationMode.LOCAL_AUTOMATION_PACKAGE)
            .withPluginsFromClasspath().build()) {
            cacheRoot = executionEngine.getExecutionEngineContext().require(LocalApResourceCacheRoot.class).getRoot();
            assertTrue(cacheRoot.getAbsolutePath(), cacheRoot.isDirectory());
        }

        assertFalse(cacheRoot.getAbsolutePath(), cacheRoot.exists());
    }
}
