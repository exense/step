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
package step.attachments;

import org.bson.types.ObjectId;
import step.automation.packages.ApResourceProvider;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionFileHandle;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class FileResolver {

    public static final String ATTACHMENT_PREFIX = "attachment:";
    public static final String RESOURCE_PREFIX = "resource:";
    public static final String AP_RESOURCE_PREFIX = "apResource:";
    public static final String RESOURCE_PATH_SEPARATOR = ":";

    /**
     * The {@code <apId>} standing for "the automation package currently open in the editor", used by
     * the AP editor instead of an entity id — which only exists once a package is deployed.
     * <p>
     * An {@code apResource:local:} reference is <b>in-memory only</b>: the YAML descriptor holds the
     * plain relative path, and {@code AutomationPackageLocalResourceMapper} maps the two on read and
     * write. It must therefore never reach a deployment, see
     * {@code AutomationPackageResourceMapper.applyResourceReference}.
     */
    public static final String LOCAL_AP_ID = "local";

    /**
     * used for direct access to files relative to the given filesystem path
     * when @{{@link FileResolver#resolve(String)} is called without any prefix
     */
    private Path unprefixedRoot = Path.of("");

    private final ResourceManager resourceManager;

    private ApResourceProvider apResourceProvider;

    public FileResolver(ResourceManager resourceManager) {
        super();
        this.resourceManager = resourceManager;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public void setUnprefixedRoot(Path pathRoot) {
        unprefixedRoot = pathRoot;
    }

    public void setApResourceProvider(ApResourceProvider apResourceProvider) {
        this.apResourceProvider = apResourceProvider;
    }

    public File resolve(String path) {
        File file;
        if (path.startsWith(ATTACHMENT_PREFIX)) {
            throw new RuntimeException("Attachments have been migrated to the ResourceManager. The reference " + path +
                " isn't valid anymore. Your attachment should be migrated to the ResourceManager.");
        } else if (path.startsWith(AP_RESOURCE_PREFIX)) {
            file = resolveApResource(path);
        } else if (path.startsWith(RESOURCE_PREFIX)) {
            file = getResourceRevisionFileHandleForPath(path).getResourceFile();
        } else {
            file = unprefixedRoot.resolve(Path.of(path)).toFile();
        }
        return file;
    }

    private File resolveApResource(String path) {
        if (apResourceProvider == null) {
            throw new RuntimeException("No ApResourceProvider is configured to resolve the reference " + path);
        }
        return apResourceProvider.resolve(extractApId(path), extractApRelativePath(path));
    }

    public static String resolveResourceId(String path) {
        String resourceId;
        if (path != null && path.startsWith(RESOURCE_PREFIX)) {
            String subResourcePath = extractResourceSubPath(path);
            resourceId = subResourcePath.split(RESOURCE_PATH_SEPARATOR)[0];
        } else {
            resourceId = null;
        }
        return resourceId;
    }

    public static String resolveRevisionId(String path) {
        String revisionId = null;
        if (path != null && path.startsWith(RESOURCE_PREFIX)) {
            String subResourcePath = extractResourceSubPath(path);
            String[] split = subResourcePath.split(RESOURCE_PATH_SEPARATOR);
            if (split.length == 2) {
                revisionId = split[1];
            }
        }
        if (revisionId == null || !ObjectId.isValid(revisionId)) {
            throw new RuntimeException("Invalid revision path: " + path);
        }
        return revisionId;
    }

    public static boolean isResource(String path) {
        return path != null && path.startsWith(RESOURCE_PREFIX);
    }

    public static boolean isResourceRevision(String path) {
        return path != null && path.startsWith(RESOURCE_PREFIX) && (extractResourceSubPath(path).split(RESOURCE_PATH_SEPARATOR).length == 2);
    }

    public static boolean isApResource(String path) {
        return path != null && path.startsWith(AP_RESOURCE_PREFIX);
    }

    /**
     * @return whether {@code path} is an {@code apResource:} reference to the automation package
     * currently open in the editor, rather than to a deployed one
     */
    public static boolean isLocalApResource(String path) {
        // deliberately a plain prefix test rather than extractApId, which throws on a malformed
        // reference - this is a predicate, callers use it to decide whether to look closer
        return path != null && path.startsWith(AP_RESOURCE_PREFIX + LOCAL_AP_ID + RESOURCE_PATH_SEPARATOR);
    }

    /**
     * Builds an {@code apResource:<apId>:<relativePath>} reference.
     */
    public static String createPathForApResource(String apId, String relativePath) {
        return AP_RESOURCE_PREFIX + apId + RESOURCE_PATH_SEPARATOR + relativePath;
    }

    /**
     * Builds an {@code apResource:local:<relativePath>} reference, the in-memory form used by the AP
     * editor. See {@link #LOCAL_AP_ID}.
     */
    public static String createPathForLocalApResource(String relativePath) {
        return createPathForApResource(LOCAL_AP_ID, relativePath);
    }

    /**
     * @return the {@code <apId>} of an {@code apResource:} reference (the segment between the first
     * and second {@code :}). The relative path may itself contain {@code :}, so the split is on the
     * <b>first</b> separator only — do not use {@code String.split}.
     */
    public static String extractApId(String path) {
        return apResourceSeparatorSplit(path)[0];
    }

    /**
     * @return the archive-root relative path of an {@code apResource:} reference (everything after
     * the second {@code :}), untouched. Normalisation is deferred to
     * {@link #normalizeApRelativePath(String)} at materialisation time.
     */
    public static String extractApRelativePath(String path) {
        return apResourceSeparatorSplit(path)[1];
    }

    /**
     * Splits {@code apResource:<apId>:<relativePath>} into {@code [apId, relativePath]} on the first
     * separator following the prefix. Crucially this uses {@code String.indexOf} rather than
     * {@code String.replace}: {@link #extractResourceSubPath(String)} strips the {@code resource:}
     * prefix with a <i>global</i> {@code replace}, which would corrupt any occurrence of the prefix
     * inside the path itself. Parsing by index avoids that trap.
     *
     * @throws IllegalArgumentException if {@code path} is not a well formed {@code apResource:}
     *                                  reference
     */
    private static String[] apResourceSeparatorSplit(String path) {
        if (!isApResource(path)) {
            throw new IllegalArgumentException("Not an apResource reference: " + path);
        }
        String remainder = path.substring(AP_RESOURCE_PREFIX.length());
        int separator = remainder.indexOf(RESOURCE_PATH_SEPARATOR);
        if (separator < 0) {
            throw new IllegalArgumentException("Invalid apResource reference (missing relative path): " + path);
        }
        return new String[]{remainder.substring(0, separator), remainder.substring(separator + 1)};
    }

    /**
     * Normalises an archive-root relative path (backslashes to {@code /}, strips leading {@code ./}
     * and {@code /}, collapses {@code .}/{@code ..} segments) and rejects any path that escapes its
     * root. Used both to look the entry up in the archive and to build the on-disk cache target, so
     * a {@code ..} traversal cannot reach outside {@code <cacheRoot>/<apId>/}.
     * @throws IllegalArgumentException if the path is empty or escapes the archive root. Note that a
     *                                  path the file system itself cannot represent surfaces as an
     *                                  {@code InvalidPathException}, which is one too
     */
    public static String normalizeApRelativePath(String relativePath) {
        String slashed = relativePath.replace('\\', '/');
        while (slashed.startsWith("./")) {
            slashed = slashed.substring(2);
        }
        while (slashed.startsWith("/")) {
            slashed = slashed.substring(1);
        }
        Path normalized = Path.of(slashed).normalize();
        if (normalized.isAbsolute() || normalized.startsWith("..") || normalized.toString().isEmpty()) {
            throw new IllegalArgumentException("Illegal apResource relative path (escapes the archive root): " + relativePath);
        }
        return normalized.toString().replace('\\', '/');
    }

    public static String createPathForResource(Resource resource) {
        return createPathForResourceId(resource.getId().toString());
    }

    public static String createPathForResourceId(String resourceId) {
        return RESOURCE_PREFIX + resourceId;
    }

    public static String createRevisionPathForResource(Resource resource) {
        return createPathForResourceAndRevisionId(resource.getId().toHexString(), resource.getCurrentRevisionId().toHexString());
    }

    public static String createPathForResourceAndRevisionId(String resourceId, String revisionId) {
        return RESOURCE_PREFIX + resourceId + RESOURCE_PATH_SEPARATOR + revisionId;
    }

    protected static String extractResourceSubPath(String path) {
        return path.replace(RESOURCE_PREFIX, "");
    }

    public FileHandle resolveFileHandle(String path) {
        File file;
        ResourceRevisionFileHandle resourceRevisionFileHandle;
        if (path.startsWith(ATTACHMENT_PREFIX)) {
            throw new RuntimeException("Attachments have been migrated to the ResourceManager. The reference " + path +
                " isn't valid anymore. Your attachment should be migrated to the ResourceManager.");
        } else if (path.startsWith(AP_RESOURCE_PREFIX)) {
            // A materialised AP resource is a plain file with no resource revision handle to close.
            file = resolveApResource(path);
            resourceRevisionFileHandle = null;
        } else if (path.startsWith(RESOURCE_PREFIX)) {
            resourceRevisionFileHandle = getResourceRevisionFileHandleForPath(path);
            file = resourceRevisionFileHandle.getResourceFile();
        } else {
            file = new File(path);
            resourceRevisionFileHandle = null;
        }
        return new FileHandle(file, resourceRevisionFileHandle);
    }

    private ResourceRevisionFileHandle getResourceRevisionFileHandleForPath(String path) {
        ResourceRevisionFileHandle resourceRevisionFileHandle;
        String subResourcePath = extractResourceSubPath(path);
        String[] split = subResourcePath.split(RESOURCE_PATH_SEPARATOR);
        if (split.length == 1) {
            resourceRevisionFileHandle = resourceManager.getResourceFile(split[0]);
        } else if (split.length == 2) {
            resourceRevisionFileHandle = resourceManager.getResourceFile(split[0], split[1]);
        } else {
            throw new RuntimeException("Invalid resource path: " + path);
        }
        return resourceRevisionFileHandle;
    }

    public static class FileHandle implements Closeable {

        protected final File file;
        protected final ResourceRevisionFileHandle resourceRevisionFileHandle;

        public FileHandle(File file, ResourceRevisionFileHandle resourceRevisionFileHandle) {
            super();
            this.file = file;
            this.resourceRevisionFileHandle = resourceRevisionFileHandle;
        }

        public File getFile() {
            return file;
        }

        @Override
        public void close() throws IOException {
            if (resourceRevisionFileHandle != null) {
                resourceRevisionFileHandle.close();
            }
        }
    }
}
