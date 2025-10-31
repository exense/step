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
package step.core.maven;

import step.resources.ResourceOrigin;
import step.resources.ResourceOriginType;

import java.util.Objects;

public class MavenArtifactIdentifier implements ResourceOrigin {

    public static final String MVN_PREFIX = "mvn";
    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private String type;

    public MavenArtifactIdentifier() {
    }

    public MavenArtifactIdentifier(String groupId, String artifactId, String version, String classifier, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.type = type;
    }

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

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(getGroupId());
        buffer.append(':').append(getArtifactId());
        if (getClassifier() != null && !getClassifier().isEmpty()) {
            buffer.append(':').append(getClassifier());
        }
        buffer.append(':').append(getVersion());
        if (getType() != null && !getType().isEmpty()) {
            buffer.append(':').append(getType());
        }
        return buffer.toString();
    }

    public boolean isSnapshot(){
        return getVersion() != null && getVersion().endsWith("-SNAPSHOT");
    }

    public boolean isModifiable(){
        return isSnapshot();
    }

    @Override
    public boolean isIdentifiable() {
        return true;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        MavenArtifactIdentifier that = (MavenArtifactIdentifier) object;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId) && Objects.equals(version, that.version) && Objects.equals(classifier, that.classifier) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, classifier, type);
    }

    /**
     * @param shortString the maven identifier in string format ('mvn:artifactId:groupId:version:classifier:type'). Classifier and type are optional
     */
    public static MavenArtifactIdentifier fromShortString(String shortString){
        if(isMvnIdentifierShortString(shortString)) {
            String[] split = shortString.split(":");
            return new MavenArtifactIdentifier(split[1], split[2], split[3], split.length >= 5 ? split[4] : null, split.length >= 6 ? split[5] : null);
        } else {
            throw new IllegalArgumentException("Invalid maven identifier: " + shortString);
        }
    }

    public static boolean isMvnIdentifierShortString(String shortString){
        return shortString != null && shortString.startsWith(MVN_PREFIX + ":");
    }
    
    public String toShortString() {
        String res = String.format(MVN_PREFIX + ":%s:%s:%s", getGroupId(),  this.getArtifactId(), getVersion());
        if (this.getClassifier() != null) {
            res += ":" + this.getClassifier();
        }
        if (this.getType() != null) {
            res +=  ":" + this.getType();
        }
        return res;
    }

    @Override
    public ResourceOriginType getOriginType() {
        return ResourceOriginType.mvn;
    }

    @Override
    public String toStringRepresentation() {
        return toShortString();
    }
}
