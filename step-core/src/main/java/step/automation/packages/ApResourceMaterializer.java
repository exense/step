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
import com.google.common.util.concurrent.Striped;
import step.attachments.FileResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Materialises a single entry of an automation package archive into
 * {@code <cacheRoot>/<apId>/<relativePath>} on the local filesystem, lazily and idempotently.
 * <p>
 * Design points (see the {@code apResource:} plan):
 * <ul>
 *     <li><b>Fast path.</b> If the target already exists the archive is never opened — the
 *     {@code archiveFileSupplier} is only invoked on a genuine cache miss.</li>
 *     <li><b>Stable path.</b> The path is keyed by {@code apId} only (no content/version segment),
 *     so the grid derives a stable {@code fileId} and a redeploy re-materialises into the same path
 *     with a fresh {@code lastModified} — the "same file, new content" signal.</li>
 *     <li><b>Atomic visibility.</b> Content is written to a temporary sibling and then atomically
 *     renamed, so a concurrent reader (or the grid version computation) never sees partial content.</li>
 *     <li><b>Per-entry locking.</b> A striped lock (bounded, no per-entry leak) serialises concurrent
 *     materialisation of the same entry.</li>
 * </ul>
 */
public class ApResourceMaterializer {

    private static final String TMP_PREFIX = ".ap-";

    private final Striped<Lock> locks = Striped.lock(64);

    /**
     * @param cacheRoot       the materialisation root (e.g. {@code data/AP_cache})
     * @param apId            the automation package entity id
     * @param relativePath    the archive-root relative path of the entry
     * @param archiveSupplier supplies the automation package archive; invoked only on a cache miss.
     *                        The returned archive is closed by this method once the entry has been
     *                        materialised.
     * @return the materialised file (or directory), never {@code null}
     * @throws ApResourceNotFoundException if the entry is absent from the archive
     */
    public File materialize(File cacheRoot, String apId, String relativePath, Supplier<AutomationPackageArchive> archiveSupplier) {
        Objects.requireNonNull(cacheRoot, "cacheRoot must not be null");
        Objects.requireNonNull(apId, "apId must not be null");
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        Objects.requireNonNull(archiveSupplier, "archiveSupplier must not be null");
        String normalized = FileResolver.normalizeApRelativePath(relativePath);
        File target = new File(ApResourceCache.apDirectory(cacheRoot, apId), normalized);
        if (target.exists()) {
            return target;
        }
        Lock lock = locks.get(target.getAbsolutePath());
        lock.lock();
        try {
            if (target.exists()) {
                // Materialised by another thread while we waited on the lock.
                return target;
            }
            Files.createDirectories(target.toPath().getParent());
            try (AutomationPackageArchive archive = archiveSupplier.get()) {
                URL url = archive.getResource(normalized);
                if (url == null) {
                    throw new ApResourceNotFoundException("Resource '" + relativePath
                        + "' not found in automation package " + apId);
                }
                if (ClassLoaderResourceFilesystem.isDirectory(url)) {
                    materializeDirectory(url, target);
                } else {
                    materializeFile(url, target);
                }
            }
            return target;
        } catch (RuntimeException e) {
            // ApResourceNotFoundException and provider/wiring errors already carry a clear message —
            // propagate as-is rather than burying them in a generic wrapper.
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to materialise apResource '" + relativePath
                + "' of automation package " + apId, e);
        } finally {
            lock.unlock();
        }
    }

    private void materializeFile(URL url, File target) throws IOException {
        Path parent = target.toPath().getParent();
        Path tmp = Files.createTempFile(parent, TMP_PREFIX, ".tmp");
        try {
            try (InputStream in = openStreamWithoutCaching(url)) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            atomicMove(tmp, target.toPath());
        } finally {
            // No-op on the success path: atomicMove has renamed tmp onto target, so it no longer
            // exists. This only deletes a stray temp left behind when the copy or move above threw.
            Files.deleteIfExists(tmp);
        }
    }

    private void materializeDirectory(URL url, File target) throws Exception {
        try (ClassLoaderResourceFilesystem.ExtractedDirectory extracted = ClassLoaderResourceFilesystem.extractDirectory(url)) {
            Path parent = target.toPath().getParent();
            Path tmp = Files.createTempDirectory(parent, TMP_PREFIX);
            try {
                copyTree(extracted.directory.toPath(), tmp);
                atomicMove(tmp, target.toPath());
            } finally {
                // No-op on the success path: atomicMove has renamed tmp onto target, so it no longer
                // exists. This only removes a stray temp tree left behind when the copy or move threw.
                if (Files.exists(tmp)) {
                    FileHelper.deleteFolder(tmp.toFile());
                }
            }
        }
    }

    /**
     * Recursively copies a directory tree. Domain-free filesystem utility — candidate to be promoted
     * to {@code ch.exense.commons.io.FileHelper} in exense-commons (which has no {@code Path}-based
     * recursive copy today) and shared with {@code ClassLoaderResourceFilesystem.extractDirectory},
     * which hand-rolls the same walk-and-copy. Kept local until there is a second consumer.
     */
    private static void copyTree(Path source, Path destination) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            walk.forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path targetPath = destination.resolve(relative.toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * Atomic move with a defensive fallback. Domain-free filesystem utility — candidate to be
     * promoted to {@code ch.exense.commons.io.FileHelper} in exense-commons (which has no atomic move
     * today). Kept local until there is a second consumer.
     */
    /**
     * Opens a stream for {@code url} without caching. For a {@code jar:} URL the default
     * {@link java.net.JarURLConnection} caches the underlying {@code JarFile}, which keeps the archive
     * file locked (notably on Windows) even after the archive's class loader is closed — blocking a
     * later delete or redeploy. Disabling caching releases the handle when the stream is closed.
     */
    private static InputStream openStreamWithoutCaching(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Defensive fallback. We always create the temp in the target's own parent directory, so
            // source and target share a filesystem and the usual cause of this exception (a
            // cross-filesystem move, EXDEV) cannot occur here. It can still be thrown by exotic
            // java.nio providers that simply don't implement atomic move (some FUSE/overlay or
            // network filesystems), in which case a plain same-filesystem move (rename) is used.
            Files.move(source, target);
        }
    }
}
