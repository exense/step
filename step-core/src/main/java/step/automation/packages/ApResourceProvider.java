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

import java.io.File;
import java.nio.file.Path;

/**
 * Resolves a file embedded in an automation package archive, referenced through the
 * {@code apResource:<apId>:<relativePath>} scheme, into a concrete {@link File} on the local
 * filesystem.
 * <p>
 * Consumers ({@code ExcelFileLookup}, {@code FileReaderDataPool}, the grid file registration in
 * {@code AbstractFunctionType}, ...) only ever receive a {@link File}, never a stream, so the
 * implementation is expected to materialise the archive entry lazily on first request. See the
 * {@code AutomationPackageResourceProvider} implementation, used by main, isolated and local IDE
 * executions alike.
 */
public interface ApResourceProvider {

    /**
     * Resolve, materialising if necessary, the archive entry {@code relativePath} of the automation
     * package identified by {@code apId}.
     *
     * @param apId         the automation package entity id
     * @param relativePath the archive-root relative path of the entry
     * @return the materialised file; never {@code null}
     * @throws RuntimeException if the entry does not exist or cannot be materialised. Implementations
     *                          must throw rather than return {@code null}: a {@code null} would make
     *                          {@code AbstractFunctionType} silently fall back to the global resolver
     *                          and mask a genuine (e.g. isolated-mode) error.
     */
    File resolve(String apId, String relativePath);

    /**
     * Whether new files may be created in an automation package here, and where.
     * <p>
     * Only the editor can answer anything but {@code null}: everywhere else an automation package is an
     * immutable archive. It is what lets a keyword type create the script of a new keyword inside the
     * package being edited rather than as a Step {@code Resource} - see
     * {@code AbstractScriptFunctionType.setupScriptFileAsResource}, which has no other way of knowing
     * where it runs.
     *
     * @return the root directory of the automation package open in the editor, or {@code null} where
     * automation packages are immutable (a Step server, an execution)
     * @throws IllegalStateException if this <i>is</i> an editor but no automation package is open -
     *                               creating a file into a package that is not there is an error, not
     *                               a reason to fall back to the server behaviour
     */
    default Path getEditableRoot() {
        return null;
    }
}
