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
package step.plans.parser.yaml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import step.core.yaml.PatchingContext;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(VersionedYamlPlan.VERSION_FIELD_NAME)
public class VersionedYamlPlan extends YamlPlan {

    // this name should be kept untouched to support the migrations for old versions
    public static final String VERSION_FIELD_NAME = "version";

    private String version;

    public VersionedYamlPlan(PatchingContext context, String version) {

        super(context);
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
