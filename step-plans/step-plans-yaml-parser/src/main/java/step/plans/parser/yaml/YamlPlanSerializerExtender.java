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

import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

import java.util.ArrayList;
import java.util.List;

public interface YamlPlanSerializerExtender {

    default List<YamlArtefactFieldSerializationProcessor> getSerializationExtensions(){
        return new ArrayList<>();
    }

    default List<YamlArtefactFieldDeserializationProcessor> getDeserializationExtensions(){
        return new ArrayList<>();
    }

    default List<JsonSchemaFieldProcessor> getJsonSchemaExtensions() {
        return new ArrayList<>();
    }

    default ExtendedYamlPlanVersion getYamlPlanVersion() {
        return null;
    }

    class ExtendedYamlPlanVersion {
        private final int priority;
        private final YamlPlanVersions.YamlPlanVersion version;

        public ExtendedYamlPlanVersion(int priority, YamlPlanVersions.YamlPlanVersion version) {
            this.priority = priority;
            this.version = version;
        }

        public int getPriority() {
            return priority;
        }

        public YamlPlanVersions.YamlPlanVersion getVersion() {
            return version;
        }
    }
}
