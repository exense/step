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
import step.core.filebrowser.FileDescriptors;
import step.core.filebrowser.FileDescriptor;
import step.core.filebrowser.DirectoryListing;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Read-only view on the content of an automation package archive, backing the UI file browser used to
 * pick a file of the package and the download of a single entry. The reference to be stored for the
 * picked file is not built here but by the injected builder, so that the same browser serves the
 * deployed package on the controller ({@code apResource:<apId>:<path>}) and the package being edited
 * in the local IDE (a plain relative path).
 * <p>
 * It operates on the archive <b>file</b> only (a zip/jar, or an exploded directory for the local /
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
     * Which kind of entry a listing should retain, the archive counterpart of the
     * {@code filesOnly} / {@code dirsOnly} filters of the local file system browser.
     */
    public enum EntryFilter {

        ALL,
        FILES_ONLY,
        DIRECTORIES_ONLY;

        /**
         * Maps the {@code filesOnly} / {@code dirsOnly} query parameter pair, which is the form the
         * clients use, onto the corresponding filter.
         *
         * @throws IllegalArgumentException if both are requested at once
         */
        public static EntryFilter of(boolean filesOnly, boolean dirsOnly) {
            if (filesOnly && dirsOnly) {
                throw new IllegalArgumentException("Cannot specify both filesOnly and dirsOnly");
            }
            return filesOnly ? FILES_ONLY : dirsOnly ? DIRECTORIES_ONLY : ALL;
        }

        private Predicate<FileDescriptor> asPredicate() {
            return switch (this) {
                case FILES_ONLY -> entry -> !entry.directory();
                case DIRECTORIES_ONLY -> FileDescriptor::directory;
                case ALL -> entry -> true;
            };
        }
    }

    /**
     * Lists the direct children of a directory of the archive, keeping every entry.
     *
     * @see #browse(File, String, Function, EntryFilter)
     */
    public static DirectoryListing browse(File archiveFile, String relativePath,
                                          Function<String, String> resourceReferenceBuilder) {
        return browse(archiveFile, relativePath, resourceReferenceBuilder, EntryFilter.ALL);
    }

    /**
     * Lists the direct children of a directory of the archive.
     * <p>
     * The {@code relativePath} is where the browser should <i>open</i>, which is typically the value
     * currently held by the field being edited - normally a file. It is therefore resolved as follows:
     * a directory is listed itself, a file lists its parent directory so that the client can preselect it,
     * and anything else is reported as not found rather than silently falling back to an existing
     * ancestor, which would hide a dangling reference behind a successful listing. The directory that was
     * effectively listed is reported by {@link DirectoryListing#path()}.
     *
     * @param archiveFile              the automation package archive: a zip/jar file or an exploded directory
     * @param relativePath             the archive-root relative path to open at; {@code null}, blank,
     *                                 {@code /} or {@code .} for the root
     * @param resourceReferenceBuilder builds the reference of an entry from its archive-root relative
     *                                 path. This is what makes the browser usable in both modes without
     *                                 knowing about them: the controller passes
     *                                 {@code path -> FileResolver.createPathForApResource(apId, path)},
     *                                 the local IDE the identity
     * @param filter                   which kind of entry to retain
     * @throws ApResourceNotFoundException if the archive itself, or {@code relativePath} in it, cannot
     *                                     be found
     * @throws RuntimeException            if {@code relativePath} escapes the archive root
     */
    public static DirectoryListing browse(File archiveFile, String relativePath,
                                          Function<String, String> resourceReferenceBuilder,
                                          EntryFilter filter) {
        Objects.requireNonNull(archiveFile, "archiveFile must not be null");
        Objects.requireNonNull(resourceReferenceBuilder, "resourceReferenceBuilder must not be null");
        Objects.requireNonNull(filter, "filter must not be null");
        assertArchiveExists(archiveFile);
        ArchiveIndex index = ArchiveIndex.of(archiveFile);
        String directoryPath = index.directoryToList(toRootRelativePath(relativePath), archiveFile);
        return new DirectoryListing(directoryPath, parentOf(directoryPath),
            referenceOfDirectory(directoryPath, resourceReferenceBuilder),
            index.list(directoryPath, resourceReferenceBuilder, filter));
    }

    /**
     * @return the reference of the listed directory itself, or {@code null} for the archive root: the root
     * has no relative path, hence no reference that could be resolved back
     */
    private static String referenceOfDirectory(String directoryPath,
                                               Function<String, String> resourceReferenceBuilder) {
        return directoryPath.isEmpty() ? null : resourceReferenceBuilder.apply(directoryPath);
    }

    /**
     * Opens the content of a single archive entry for reading. The returned {@link ApResourceStream}
     * owns the underlying archive handle and must be closed by the caller.
     *
     * @param archiveFile  the automation package archive: a zip/jar file or an exploded directory
     * @param relativePath the archive-root relative path of the entry
     * @throws ApResourceNotFoundException if the archive or the entry cannot be found
     * @throws IllegalArgumentException    if the entry is a directory - directories have no content to
     *                                     stream, use {@link #browse(File, String, Function)} instead
     * @throws RuntimeException            if {@code relativePath} is empty or escapes the archive root
     */
    public static ApResourceStream openEntry(File archiveFile, String relativePath) {
        Objects.requireNonNull(archiveFile, "archiveFile must not be null");
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        assertArchiveExists(archiveFile);
        String normalized = FileResolver.normalizeApRelativePath(relativePath);
        return archiveFile.isDirectory() ? openDirectoryEntry(archiveFile, normalized)
            : openZipEntry(archiveFile, normalized);
    }

    private static ApResourceStream openDirectoryEntry(File archiveDirectory, String normalized) {
        File file = new File(archiveDirectory, normalized);
        if (!file.exists()) {
            throw entryNotFound(normalized, archiveDirectory);
        }
        if (file.isDirectory()) {
            throw isADirectory(normalized);
        }
        try {
            return new ApResourceStream(file.getName(), file.length(), new FileInputStream(file), null);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read '" + normalized + "' in " + nameOfArchive(archiveDirectory), e);
        }
    }

    private static ApResourceStream openZipEntry(File archiveFile, String normalized) {
        ZipFile zip;
        try {
            zip = new ZipFile(archiveFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to open the automation package archive " + nameOfArchive(archiveFile), e);
        }
        try {
            // Note: getEntry() falls back to the directory entry ('<name>/') when only that one exists,
            // which is how a directory is detected here.
            ZipEntry entry = zip.getEntry(normalized);
            if (entry == null) {
                throw entryNotFound(normalized, archiveFile);
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
            throw new RuntimeException("Unable to read '" + normalized + "' in " + nameOfArchive(archiveFile), e);
        }
    }

    private static void assertArchiveExists(File archiveFile) {
        if (!archiveFile.exists()) {
            throw new ApResourceNotFoundException("The automation package archive could not be found: "
                + archiveFile.getAbsolutePath());
        }
    }

    private static ApResourceNotFoundException entryNotFound(String normalized, File archiveFile) {
        return new ApResourceNotFoundException("'" + normalized + "' not found in the automation package "
            + nameOfArchive(archiveFile));
    }

    /**
     * @return the name of the archive, to name it in a message about one of its entries. The absolute
     * path is deliberately left out of those: the caller browsing the package has no use for the server
     * side storage location. It is kept in {@link #assertArchiveExists(File)} only, where a missing
     * archive is a server side problem rather than a bad request.
     */
    private static String nameOfArchive(File archiveFile) {
        return archiveFile.getName();
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
                    index.indexDirectory(archiveFile);
                } else {
                    index.indexZip(archiveFile);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to read the content of the automation package archive "
                    + archiveFile.getAbsolutePath(), e);
            }
            return index;
        }

        private void indexDirectory(File root) throws IOException {
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
         * @return the directory to list for {@code path}: the archive root for an empty path, {@code path}
         * itself if it is a directory, and its parent directory if it is a file - the browser being
         * normally opened on the file the edited field currently holds
         * @throws ApResourceNotFoundException if {@code path} is neither
         */
        String directoryToList(String path, File archiveFile) {
            if (path.isEmpty() || directories.contains(path)) {
                return path;
            }
            if (files.containsKey(path)) {
                return parentOf(path);
            }
            throw entryNotFound(path, archiveFile);
        }

        List<FileDescriptor> list(String directoryPath, Function<String, String> resourceReferenceBuilder,
                                  EntryFilter filter) {
            List<FileDescriptor> entries = new ArrayList<>();
            directories.stream().filter(directory -> isChildOf(directoryPath, directory))
                .forEach(directory -> entries.add(toFileDescriptor(directory, true, null, resourceReferenceBuilder)));
            files.forEach((file, size) -> {
                // an explicit directory entry wins over a file entry of the same path
                if (isChildOf(directoryPath, file) && !directories.contains(file)) {
                    entries.add(toFileDescriptor(file, false, size, resourceReferenceBuilder));
                }
            });
            entries.removeIf(filter.asPredicate().negate());
            entries.sort(FileDescriptors.byDirectoryThenName());
            return entries;
        }

        private static boolean isChildOf(String directoryPath, String path) {
            return directoryPath.equals(parentOf(path));
        }

        /**
         * Create a FileDescriptor from provided path and options
         * {@code hidden} and {@code symlink} are always {@code false}: an archive carries no such
         * attribute, and deriving them from the file system in the exploded directory case would make the
         * two archive flavours describe the same package differently.
         */
        private static FileDescriptor toFileDescriptor(String path, boolean directory, Long size,
                                                       Function<String, String> resourceReferenceBuilder) {
            return new FileDescriptor(nameOf(path), path, directory, !directory, false, false, size,
                resourceReferenceBuilder.apply(path));
        }
    }
}
