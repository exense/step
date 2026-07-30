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

import step.attachments.ApResourceNotFoundException;
import step.attachments.FileResolver;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Read-only view on the content of an automation package archive, backing the UI file browser used to
 * pick an {@code apResource:} reference and the download of a single entry.
 * <p>
 * It operates on the archive <b>file</b> only (a zip/jar, or an exploded folder for the local /
 * AP editor mode), deliberately not on {@link AutomationPackageArchive}: browsing needs the complete
 * entry list, which a {@link ClassLoader}-based archive cannot enumerate, and it needs no class
 * loading at all.
 * <p>
 * Nothing is materialised into the {@code apResource:} cache here — that cache is owned by the
 * execution path ({@link ApResourceMaterializer}) and a UI browse or download must not pollute it,
 * nor extract a whole directory just to answer a listing.
 */
public class ApResourceBrowser {

    private static final String SEPARATOR = "/";

    private ApResourceBrowser() {
    }

    /**
     * Lists the direct children of a folder of the archive.
     * <p>
     * The {@code relativePath} is where the browser should <i>open</i>, which is typically the value
     * currently held by the field being edited. It is therefore resolved leniently: the folder itself
     * if it is a directory, its parent folder if it is a file, and otherwise the closest existing
     * ancestor (down to the archive root). The folder that was effectively listed is reported by
     * {@link ApResourceFolderContent#path()}.
     *
     * @param apId         the automation package id, used to build the reference of each entry
     * @param archiveFile  the automation package archive: a zip/jar file or an exploded folder
     * @param relativePath the archive-root relative path to open at; {@code null} or empty for the root
     * @throws ApResourceNotFoundException if the archive itself cannot be found
     * @throws RuntimeException            if {@code relativePath} escapes the archive root
     */
    public static ApResourceFolderContent browse(String apId, File archiveFile, String relativePath) {
        Objects.requireNonNull(apId, "apId must not be null");
        Objects.requireNonNull(archiveFile, "archiveFile must not be null");
        assertArchiveExists(apId, archiveFile);
        ArchiveIndex index = ArchiveIndex.of(archiveFile);
        String folder = index.closestFolder(toRootRelativePath(relativePath));
        return new ApResourceFolderContent(apId, folder, parentOf(folder), index.list(folder, apId));
    }

    /**
     * Opens the content of a single archive entry for reading. The returned {@link ApResourceStream}
     * owns the underlying archive handle and must be closed by the caller.
     *
     * @param apId         the automation package id, used for error messages only
     * @param archiveFile  the automation package archive: a zip/jar file or an exploded folder
     * @param relativePath the archive-root relative path of the entry
     * @throws ApResourceNotFoundException if the archive or the entry cannot be found
     * @throws IllegalArgumentException    if the entry is a directory - directories have no content to
     *                                     stream, use {@link #browse(String, File, String)} instead
     * @throws RuntimeException            if {@code relativePath} is empty or escapes the archive root
     */
    public static ApResourceStream openEntry(String apId, File archiveFile, String relativePath) {
        Objects.requireNonNull(apId, "apId must not be null");
        Objects.requireNonNull(archiveFile, "archiveFile must not be null");
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        assertArchiveExists(apId, archiveFile);
        String normalized = FileResolver.normalizeApRelativePath(relativePath);
        return archiveFile.isDirectory() ? openFolderEntry(apId, archiveFile, normalized)
            : openZipEntry(apId, archiveFile, normalized);
    }

    private static ApResourceStream openFolderEntry(String apId, File archiveFolder, String normalized) {
        File file = new File(archiveFolder, normalized);
        if (!file.exists()) {
            throw entryNotFound(apId, normalized);
        }
        if (file.isDirectory()) {
            throw isADirectory(normalized);
        }
        try {
            return new ApResourceStream(file.getName(), file.length(), new FileInputStream(file), null);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read apResource '" + normalized + "' of automation package " + apId, e);
        }
    }

    private static ApResourceStream openZipEntry(String apId, File archiveFile, String normalized) {
        ZipFile zip;
        try {
            zip = new ZipFile(archiveFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to open the archive of automation package " + apId, e);
        }
        try {
            // Note: getEntry() falls back to the directory entry ('<name>/') when only that one exists,
            // which is how a folder is detected here.
            ZipEntry entry = zip.getEntry(normalized);
            if (entry == null) {
                throw entryNotFound(apId, normalized);
            }
            if (entry.isDirectory()) {
                throw isADirectory(normalized);
            }
            return new ApResourceStream(nameOf(normalized), entry.getSize(), zip.getInputStream(entry), zip);
        } catch (RuntimeException e) {
            closeQuietly(zip);
            throw e;
        } catch (IOException e) {
            closeQuietly(zip);
            throw new RuntimeException("Unable to read apResource '" + normalized + "' of automation package " + apId, e);
        }
    }

    private static void assertArchiveExists(String apId, File archiveFile) {
        if (!archiveFile.exists()) {
            throw new ApResourceNotFoundException("The archive of automation package " + apId
                + " could not be found: " + archiveFile.getAbsolutePath());
        }
    }

    private static ApResourceNotFoundException entryNotFound(String apId, String normalized) {
        return new ApResourceNotFoundException("Resource '" + normalized + "' not found in automation package " + apId);
    }

    private static IllegalArgumentException isADirectory(String normalized) {
        return new IllegalArgumentException("The apResource '" + normalized
            + "' is a directory and has no content to download");
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // best effort: we are already propagating the original failure
        }
    }

    /**
     * @return the normalised archive-root relative path, or an empty string for the archive root
     * ({@code null}, blank, {@code "/"} or {@code "."})
     */
    private static String toRootRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()
            || relativePath.equals(SEPARATOR) || relativePath.equals(".")) {
            return "";
        }
        return FileResolver.normalizeApRelativePath(relativePath);
    }

    /**
     * @return the parent path of {@code path}, an empty string for a top level entry, or {@code null}
     * for the archive root itself
     */
    private static String parentOf(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        int lastSeparator = path.lastIndexOf(SEPARATOR);
        return lastSeparator < 0 ? "" : path.substring(0, lastSeparator);
    }

    private static String nameOf(String path) {
        int lastSeparator = path.lastIndexOf(SEPARATOR);
        return lastSeparator < 0 ? path : path.substring(lastSeparator + 1);
    }

    /**
     * The content of a single archive entry, together with the archive handle it was read from. Both
     * are released on {@link #close()}.
     */
    public static class ApResourceStream implements Closeable {

        private final String name;
        private final long size;
        private final InputStream inputStream;
        private final Closeable archive;

        private ApResourceStream(String name, long size, InputStream inputStream, Closeable archive) {
            this.name = name;
            this.size = size;
            this.inputStream = inputStream;
            this.archive = archive;
        }

        public String getName() {
            return name;
        }

        /**
         * @return the size in bytes, or a negative value if the archive does not declare it
         */
        public long getSize() {
            return size;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            try {
                inputStream.close();
            } finally {
                if (archive != null) {
                    archive.close();
                }
            }
        }
    }

    /**
     * The flattened content of an archive: every entry path (always {@code /} separated, without
     * leading or trailing separator) mapped to its size, plus the set of directories - including the
     * ones only implied by a file path, since zip archives are not required to carry explicit
     * directory entries.
     */
    private static class ArchiveIndex {

        private final Map<String, Long> files = new HashMap<>();
        private final Set<String> directories = new HashSet<>();

        static ArchiveIndex of(File archiveFile) {
            ArchiveIndex index = new ArchiveIndex();
            try {
                if (archiveFile.isDirectory()) {
                    index.indexFolder(archiveFile);
                } else {
                    index.indexZip(archiveFile);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to read the content of the automation package archive "
                    + archiveFile.getAbsolutePath(), e);
            }
            return index;
        }

        private void indexFolder(File root) throws IOException {
            Path rootPath = root.toPath();
            try (Stream<Path> walk = Files.walk(rootPath)) {
                walk.filter(path -> !path.equals(rootPath)).forEach(path -> {
                    String relative = toRelativePath(rootPath.relativize(path));
                    if (Files.isDirectory(path)) {
                        directories.add(relative);
                    } else {
                        files.put(relative, path.toFile().length());
                    }
                });
            }
        }

        /**
         * Joins the segments explicitly rather than replacing {@link File#separatorChar}: a file name
         * may legitimately contain a backslash on unix, which a blind replace would turn into a
         * spurious path separator.
         */
        private static String toRelativePath(Path relative) {
            return StreamSupport.stream(relative.spliterator(), false)
                .map(Path::toString).collect(Collectors.joining(SEPARATOR));
        }

        private void indexZip(File archiveFile) throws IOException {
            try (ZipFile zip = new ZipFile(archiveFile)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = normalizeEntryName(entry.getName());
                    if (name == null) {
                        continue;
                    }
                    if (entry.isDirectory()) {
                        directories.add(name);
                    } else {
                        // a zip entry may not declare its size (-1), which we expose as 'unknown'
                        long size = entry.getSize();
                        files.put(name, size >= 0 ? size : null);
                    }
                    addAncestorDirectories(name);
                }
            }
        }

        /**
         * @return the normalised entry name, or {@code null} for the root itself and for any entry
         * whose path is not browsable ({@code .} / {@code ..} segments), which we simply hide rather
         * than exposing a reference that could not be resolved later on
         */
        private static String normalizeEntryName(String entryName) {
            String name = entryName.replace('\\', '/');
            while (name.startsWith(SEPARATOR)) {
                name = name.substring(1);
            }
            while (name.endsWith(SEPARATOR)) {
                name = name.substring(0, name.length() - 1);
            }
            if (name.isEmpty()) {
                return null;
            }
            for (String segment : name.split(SEPARATOR)) {
                if (segment.equals(".") || segment.equals("..") || segment.isEmpty()) {
                    return null;
                }
            }
            return name;
        }

        private void addAncestorDirectories(String path) {
            String parent = parentOf(path);
            while (parent != null && !parent.isEmpty()) {
                if (!directories.add(parent)) {
                    // this ancestor - and hence all of its own ancestors - is already indexed
                    return;
                }
                parent = parentOf(parent);
            }
        }

        /**
         * @return the closest folder of the archive to open at: {@code path} itself if it is a
         * directory, otherwise its closest ancestor directory, down to the root (empty string)
         */
        String closestFolder(String path) {
            String current = path;
            while (current != null && !current.isEmpty()) {
                if (directories.contains(current)) {
                    return current;
                }
                current = parentOf(current);
            }
            return "";
        }

        List<ApResourceEntry> list(String folder, String apId) {
            List<ApResourceEntry> entries = new ArrayList<>();
            directories.stream().filter(directory -> isChildOf(folder, directory))
                .forEach(directory -> entries.add(toEntry(directory, true, null, apId)));
            files.forEach((file, size) -> {
                // an explicit directory entry wins over a file entry of the same path
                if (isChildOf(folder, file) && !directories.contains(file)) {
                    entries.add(toEntry(file, false, size, apId));
                }
            });
            entries.sort(Comparator.comparing((ApResourceEntry entry) -> !entry.directory())
                .thenComparing(ApResourceEntry::name, String.CASE_INSENSITIVE_ORDER));
            return entries;
        }

        private static boolean isChildOf(String folder, String path) {
            return folder.equals(parentOf(path));
        }

        private static ApResourceEntry toEntry(String path, boolean directory, Long size, String apId) {
            return new ApResourceEntry(nameOf(path), path, directory, size,
                FileResolver.createPathForApResource(apId, path));
        }
    }
}
