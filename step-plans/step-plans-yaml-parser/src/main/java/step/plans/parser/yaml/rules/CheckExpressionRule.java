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
package step.plans.parser.yaml.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.Check;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.deserializers.YamlArtefactFieldDeserializationProcessor;
import step.plans.parser.yaml.serializers.YamlArtefactFieldSerializationProcessor;

import java.util.Map;

/**
 * The 'expression' field is 'Check' artefact is dynamic value,
 * but in yaml we want to define it always as expression string (to avoid nested "expression.expression")
 */
public class CheckExpressionRule implements ArtefactFieldConversionRule {

    private static final Logger log = LoggerFactory.getLogger(CheckExpressionRule.class);

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (aClass, field, fieldMetadata, jsonObjectBuilder, list) -> {
            if(Check.class.isAssignableFrom(field.getDeclaringClass()) && field.getName().equals(YamlPlanFields.CHECK_EXPRESSION_ORIGINAL_FIELD)){
                JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
                nestedPropertyParamsBuilder.add("type", "string");
                jsonObjectBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
                return true;
            }
            return false;
        };
    }

    @Override
    public YamlArtefactFieldDeserializationProcessor getArtefactFieldDeserializationProcessor() {
       return new YamlArtefactFieldDeserializationProcessor() {
           @Override
           public boolean deserializeArtefactField(String artefactClass, Map.Entry<String, JsonNode> field, ObjectNode output, ObjectCodec codec) throws JsonProcessingException {
               if ((artefactClass.equals("Check") && field.getKey().equals(YamlPlanFields.CHECK_EXPRESSION_ORIGINAL_FIELD))) {
                   ObjectNode objectNode = createObjectNode(codec);
                   objectNode.set(YamlPlanFields.DYN_VALUE_EXPRESSION_FIELD, field.getValue());
                   output.set(YamlPlanFields.CHECK_EXPRESSION_ORIGINAL_FIELD, objectNode);
                   return true;
               }
               return false;
           }
       };
    }

    @Override
    public YamlArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> {
            if (Check.class.isAssignableFrom(artefact.getClass()) && field.getName().equals(YamlPlanFields.CHECK_EXPRESSION_ORIGINAL_FIELD)) {
                DynamicValue<Boolean> value = (DynamicValue<Boolean>) field.get(artefact);
                if (!value.isDynamic()){
                    log.warn("Static values are not supported in simple plan format for " + field.getName() + " in " + artefact.getClass().getSimpleName());
                } else {
                    gen.writeStringField(YamlPlanFields.CHECK_EXPRESSION_ORIGINAL_FIELD, value.getExpression());
                }
                return true;
            }
            return false;
        };
    }
}
