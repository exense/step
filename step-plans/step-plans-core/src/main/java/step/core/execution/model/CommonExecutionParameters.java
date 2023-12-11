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
package step.core.execution.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactFilter;
import step.core.collections.serialization.EscapingDottedKeysMapDeserializer;
import step.core.collections.serialization.EscapingDottedKeysMapSerializer;
import step.core.objectenricher.EnricheableObject;
import step.core.plans.Plan;

import java.util.Map;

public abstract class CommonExecutionParameters extends AbstractOrganizableObject implements EnricheableObject {
    private static final String DEFAULT_DESCRIPTION = "Unnamed";

    @JsonSerialize(using = EscapingDottedKeysMapSerializer.class)
    @JsonDeserialize(using = EscapingDottedKeysMapDeserializer.class)
    Map<String, String> customParameters;

    String userID;

    ArtefactFilter artefactFilter;
    ExecutionMode mode;

    public CommonExecutionParameters() {
        super();
    }

    public CommonExecutionParameters(Map<String, String> customParameters, String userID, ArtefactFilter artefactFilter, ExecutionMode mode) {
        super();
        this.customParameters = customParameters;
        this.userID = userID;
        this.artefactFilter = artefactFilter;
        this.mode = mode;
    }

    public static String defaultDescription(Plan plan) {
        String description;
        Map<String, String> attributes = plan.getAttributes();
        if (attributes != null && attributes.containsKey(AbstractArtefact.NAME)) {
            description = attributes.get(AbstractArtefact.NAME);
        } else {
            AbstractArtefact root = plan.getRoot();
            if (root != null) {
                attributes = root.getAttributes();
                if (attributes != null && attributes.containsKey(AbstractArtefact.NAME)) {
                    description = attributes.get(AbstractArtefact.NAME);
                } else {
                    description = DEFAULT_DESCRIPTION;
                }
            } else {
                description = DEFAULT_DESCRIPTION;
            }
        }
        return description;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public ArtefactFilter getArtefactFilter() {
        return artefactFilter;
    }

    public void setArtefactFilter(ArtefactFilter artefactFilter) {
        this.artefactFilter = artefactFilter;
    }

    public Map<String, String> getCustomParameters() {
        return customParameters;
    }

    public void setCustomParameters(Map<String, String> customParameters) {
        this.customParameters = customParameters;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = mode;
    }
}
