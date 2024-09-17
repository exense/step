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
package step.cli;

import java.util.Objects;

public class MavenArtifactIdentifier {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;

    public MavenArtifactIdentifier(String groupId, String artifactId, String versionId, String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = versionId;
        this.classifier = classifier;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(getGroupId());
        buffer.append(':').append(getArtifactId());
        if (getClassifier() != null && !getClassifier().isEmpty()) {
            buffer.append(':').append(getClassifier());
        }
        buffer.append(':').append(getVersion());
        return buffer.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        MavenArtifactIdentifier that = (MavenArtifactIdentifier) object;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId) && Objects.equals(version, that.version) && Objects.equals(classifier, that.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, classifier);
    }
}
