/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.resources;

import step.core.maven.MavenArtifactIdentifier;

public class ResourceOriginFactory {
    public static ResourceOrigin fromStringRepresentation(String stringRepresentation) {
        if (stringRepresentation == null || stringRepresentation.isEmpty()) {
            return null;
        }

        if (stringRepresentation.startsWith(MavenArtifactIdentifier.MVN_PREFIX + ":")) {
            return MavenArtifactIdentifier.fromShortString(stringRepresentation);
        } else if (stringRepresentation.startsWith(ResourceOriginType.uploaded.name().toLowerCase())) {
            return new UploadedResourceOrigin();
        } else {
            throw new IllegalArgumentException("");
        }
    }
}
