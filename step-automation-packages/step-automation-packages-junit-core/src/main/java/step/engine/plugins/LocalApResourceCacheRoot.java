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
 * A shutdown hook does the same as a fallback, because nothing runs {@code close()} when the process is
 * killed or calls {@code System.exit} - and {@code Files.createTempDirectory} has no lifecycle of its own
 * and {@code File.deleteOnExit} would not help either, since it refuses a directory that is not empty.
 */
public class LocalApResourceCacheRoot implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LocalApResourceCacheRoot.class);

    protected static final String DIRECTORY_PREFIX = "ap-cache-local";

    private final File root;
    private final Thread shutdownHook;

    /**
     * Creates the cache directory and registers its deletion for the end of the JVM.
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
        this.shutdownHook = new Thread(this::delete, "ap-cache-local-cleanup");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public File getRoot() {
        return root;
    }

    @Override
    public void close() {
        if (delete()) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // The JVM is already shutting down, so the hook is running or has run: nothing to remove
            }
        }
        // On a failure the hook stays registered and tries again at the end of the JVM, by which time
        // whatever held a file - a keyword process, a file handle of the run - has usually let go.
    }

    /**
     * @return true if the directory is gone, whether this call removed it or it was already deleted
     */
    private synchronized boolean delete() {
        if (!root.exists()) {
            return true;
        }
        if (FileHelper.deleteFolder(root)) {
            logger.debug("Deleted the local apResource cache directory {}", root.getAbsolutePath());
            return true;
        }
        logger.warn("The local apResource cache directory {} could not be fully deleted", root.getAbsolutePath());
        return false;
    }
}
