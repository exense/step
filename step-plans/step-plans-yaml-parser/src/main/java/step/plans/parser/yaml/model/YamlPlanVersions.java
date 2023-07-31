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
package step.plans.parser.yaml.model;

import step.core.Version;

public class YamlPlanVersions {
    public static final YamlPlanVersion VERSION_1 = new YamlPlanVersion(
            new Version("1.0.0"),
            "step/plans/parser/yaml/yaml-plan-schema-1.0.json"
    );

    public static final YamlPlanVersion ACTUAL_VERSION = VERSION_1;

    public static class YamlPlanVersion {
        private final Version version;
        private final String jsonSchemaPath;

        public YamlPlanVersion(Version version, String jsonSchemaPath) {
            this.version = version;
            this.jsonSchemaPath = jsonSchemaPath;
        }

        public Version getVersion() {
            return version;
        }

        public String getJsonSchemaPath() {
            return jsonSchemaPath;
        }
    }
}
