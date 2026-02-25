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
package step.plugins.maven;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;

/**
 * Configuration class for specifying a library in one of three ways:
 * <ul>
 *   <li>Maven coordinates (groupId, artifactId, version, etc.)</li>
 *   <li>File path (path)</li>
 *   <li>Managed library name (name)</li>
 * </ul>
 * <p>
 * Only one of these three configuration methods should be used at a time.
 * </p>
 */
public class LibraryConfiguration {

    // Maven coordinates configuration
    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private String type;

    // File path configuration
    private String path;

    // Managed library name configuration
    private String managed;

    public boolean isSet() {
        return groupId != null || artifactId != null || version != null
                || classifier != null || type != null
                || path != null || managed != null;
    }

    /**
     * Validates that only one configuration method is specified.
     *
     * @throws MojoExecutionException if multiple configuration methods are specified or none is specified
     */
    public void validate() throws MojoExecutionException {
        int configuredMethods = 0;

        if (isMavenCoordinatesConfigured()) {
            configuredMethods++;
        }
        if (isPathConfigured()) {
            configuredMethods++;
        }
        if (isManagedLibraryNameConfigured()) {
            configuredMethods++;
        }

        if (configuredMethods > 1) {
            throw new MojoExecutionException(
                    "Library configuration error: Only one configuration method should be used " +
                            "(either Maven coordinates, path, or managed library name).");
        }
    }

    /**
     * Checks if Maven coordinates are configured.
     *
     * @return true if both groupId and artifactId are specified
     */
    public boolean isMavenCoordinatesConfigured() throws MojoExecutionException {
        boolean coordinatesConfigured = !StringUtils.isBlank(groupId) || !StringUtils.isBlank(artifactId) || !StringUtils.isBlank(version)
                || !StringUtils.isBlank(this.type)  || !StringUtils.isBlank(this.classifier);
        if (coordinatesConfigured && (StringUtils.isBlank(groupId) || StringUtils.isBlank(artifactId) || StringUtils.isBlank(version))) {
            throw new MojoExecutionException(
                    "Library configuration error: partial maven coordinates configuration found: groupId artifactId and version are required.");
        }
        return coordinatesConfigured;
    }


    /**
     * Checks if a file path is configured.
     *
     * @return true if path is specified
     */
    public boolean isPathConfigured() {
        return path != null && !path.isEmpty();
    }

    /**
     * Checks if a managed library name is configured.
     *
     * @return true if name is specified
     */
    public boolean isManagedLibraryNameConfigured() {
        return managed != null && !managed.isEmpty();
    }

    /**
     * Converts this configuration to a MavenArtifactIdentifier if Maven coordinates are configured.
     *
     * @return MavenArtifactIdentifier or null if Maven coordinates are not configured
     */
    public MavenArtifactIdentifier toMavenArtifactIdentifier() throws MojoExecutionException {
        if (isMavenCoordinatesConfigured()) {
            return new MavenArtifactIdentifier(groupId, artifactId, version, classifier, type);
        }
        return null;
    }

    /**
     * Returns the library file if path is configured.
     *
     * @return File object or null if path is not configured
     */
    public File toFile() {
        if (isPathConfigured()) {
            return new File(path);
        }
        return null;
    }

    // Getters and setters

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getManaged() {
        return managed;
    }

    public void setManaged(String managed) {
        this.managed = managed;
    }
}
