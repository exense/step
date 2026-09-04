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
package step.engine.plugins;

import ch.exense.commons.io.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * The temporary directory a local execution materialises its {@code apResource:} files into, and the only
 * thing that deletes it again.
 * <p>
 * It is held in the execution engine context, whose {@code close()} closes every {@link Closeable}
 * attribute it holds ({@code AbstractContext.close}), so the cache lives exactly as long as the engine
 * that owns it - one JUnit suite, one CLI invocation.
 * <p>
 * Closing the engine is deliberately the <b>only</b> trigger. Making sure the engine is closed at all when the process is
 * interrupted belongs to whoever owns the engine, which for the one path that reaches this class is
 * {@code CliShutdownHook} in the CLI.
 */
public class LocalApResourceCacheRoot implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LocalApResourceCacheRoot.class);

    protected static final String DIRECTORY_PREFIX = "ap-cache-local";

    private final File root;

    /**
     * Creates the cache directory. It is deleted by {@link #close()} and by nothing else.
     */
    public static LocalApResourceCacheRoot create() {
        try {
            return new LocalApResourceCacheRoot(Files.createTempDirectory(DIRECTORY_PREFIX).toFile());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create the local apResource cache directory", e);
        }
    }

    protected LocalApResourceCacheRoot(File root) {
        this.root = root;
    }

    public File getRoot() {
        return root;
    }

    @Override
    public void close() {
        delete();
    }

    /**
     * A failure is logged rather than thrown: a cache that could not be deleted is a temporary directory
     * left behind, not a reason to fail the run that just finished.
     *
     * @return true if the directory is gone, whether this call removed it or it was already deleted
     */
    private synchronized boolean delete() {
        if (!root.exists()) {
            return true;
        }
        if (FileHelper.deleteFolder(root)) {
            logger.info("Deleted the local apResource cache directory {}", root.getAbsolutePath());
            return true;
        }
        logger.warn("The local apResource cache directory {} could not be fully deleted", root.getAbsolutePath());
        return false;
    }
}
