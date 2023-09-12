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
import step.core.yaml.schema.JsonSchemaDefinitionCreator;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows to extend the logic of working with Yaml plans (for Step EE). To do that, there should be a class implementing the {@link YamlPlanReaderExtender}
 * and annotated with {@link YamlPlanReaderExtension}.
 * The following functionality can be extended via this class:
 * - the conversion from the Yaml plan to the {@link step.core.plans.Plan} object
 * - the serialization from {@link step.core.plans.Plan} to the Yaml format
 * - the generation of json schema for Yaml Plan format (see {@link step.plans.parser.yaml.schema.YamlPlanSchemaGenerationTool}
 * - the used json schema (for example, to switch to extended json schema in Step EE)
 */
public interface YamlPlanReaderExtender {

    /**
     * Defines the additional list of YamlArtefactFieldSerializationProcessors to be used during serialization
     * from {@link step.core.plans.Plan} to the Yaml format. This allows to add some special logic for Step EE artefacts
     * (custom field processing for Step EE artefacts).
     */
    default List<YamlArtefactFieldSerializationProcessor> getSerializationExtensions(){
        return new ArrayList<>();
    }

    /**
     * Defines the additional list of YamlArtefactFieldDeserializationProcessor to be used during reading the Yaml Plan
     * and convert it to the {@link step.core.plans.Plan}. This allows to add some special logic for Step EE artefacts
     * (custom field processing for Step EE artefacts).
     */
    default List<YamlArtefactFieldDeserializationProcessor> getDeserializationExtensions(){
        return new ArrayList<>();
    }

    /**
     * Defines the additional list of JsonSchemaFieldProcessor to be used during the json schema preparation. This allows
     * to add some custom json schema parts for Step EE artefacts (see {@link step.plans.parser.yaml.schema.YamlPlanSchemaGenerationTool}).
     */
    default List<JsonSchemaFieldProcessor> getJsonSchemaFieldProcessingExtensions() {
        return new ArrayList<>();
    }

    /**
     * Defines the additional list of YamlPlanJsonSchemaDefinitionCreator to be used to add some type reusable definitions (sub-schemas) to json the schema
     */
    default List<JsonSchemaDefinitionCreator> getJsonSchemaDefinitionsExtensions(){
        return new ArrayList<>();
    }

    /**
     * Allows to redefine the json schema (for instance, switch used json schema to extended one for Step EE).
     * There should be at max one {@link YamlPlanReaderExtender} annotated with {@link YamlPlanReaderExtension} overriding
     * the json schema (returning the non-null string via this method).
     */
    default String getJsonSchemaPath() {
        return null;
    }

}
