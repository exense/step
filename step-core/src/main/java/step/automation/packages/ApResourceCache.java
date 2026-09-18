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

import ch.exense.commons.io.FileHelper;

import java.io.File;

/**
 * Single source of truth for the on-disk layout of the {@code apResource:} materialisation cache:
 * every automation package's materialised entries live under {@code <cacheRoot>/<apId>}. Keeping the
 * layout here ensures the materialiser ({@link ApResourceMaterializer}) and the lifecycle wipes (on
 * redeploy/delete and isolated-context close) never disagree on where a package's files are.
 */
public class ApResourceCache {

    /**
     * Configuration property selecting the materialisation root for {@code apResource:} files.
     * Sibling of {@code resources.dir}; default {@link #DEFAULT_CACHE_DIR}.
     */
    public static final String CACHE_DIR_PROPERTY = "apResources.dir";
    public static final String DEFAULT_CACHE_DIR = "AP_cache";

    private ApResourceCache() {
    }

    /**
     * @return the directory holding the materialised entries of automation package {@code apId}
     */
    public static File apDirectory(File cacheRoot, String apId) {
        return new File(cacheRoot, apId);
    }

    /**
     * Deletes the materialisation directory of automation package {@code apId}, if any. No-op when
     * {@code cacheRoot} or {@code apId} is {@code null}, or the directory does not exist.
     *
     * @return {@code true} if nothing needed deleting or the deletion fully succeeded; {@code false}
     * if the directory could not be fully removed
     */
    public static boolean wipe(File cacheRoot, String apId) {
        if (cacheRoot == null || apId == null) {
            return true;
        }
        File apCacheDir = apDirectory(cacheRoot, apId);
        return !apCacheDir.exists() || FileHelper.deleteFolder(apCacheDir);
    }
}
