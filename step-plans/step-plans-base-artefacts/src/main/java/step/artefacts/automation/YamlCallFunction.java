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
package step.artefacts.automation;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.functions.StandardKeywordsLookuper;
import step.core.yaml.YamlFieldCustomCopy;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.jsonschema.JsonSchema;

import java.lang.reflect.Field;
import java.util.List;

public class YamlCallFunction extends YamlTokenSelector<CallFunction> {

    protected DynamicValue<String> resultMap = new DynamicValue<>();

    @YamlFieldCustomCopy
    protected YamlDynamicInputs inputs = new YamlDynamicInputs("{}");

    @YamlFieldCustomCopy
    protected YamlKeywordDefinition keyword = new YamlKeywordDefinition(null, null, "{}");

    @JsonSchema(customJsonSchemaProcessor = PredefinedKeywordJsonSchemaProcessor.class)
    protected String standardKeyword;

    public YamlCallFunction() {
        super(CallFunction.class);
    }

    @Override
    protected void fillArtefactFields(CallFunction res) {
        super.fillArtefactFields(res);
        if (this.inputs != null) {
            res.setArgument(this.inputs.toDynamicValue());
        }

        if (this.standardKeyword != null && this.keyword != null) {
            throw new RuntimeException("You cannot use both 'standardKeyword' and 'keyword' section to define keyword for 'callKeyword'");
        }

        if (this.standardKeyword != null) {
            res.setFunction(new DynamicValue<>(this.standardKeyword));
            res.setStandardFunction(true);
        } else if (this.keyword != null) {
            res.setFunction(this.keyword.toDynamicValue());
            res.setStandardFunction(false);
        }

        // for keywords, if nodeName is not defined or using dynamic name, we use the keyword name as default artefact name
        if (getNodeName() == null || getNodeName().isDynamic()) {
            String keywordName = getKeywordName();
            res.addAttribute(AbstractOrganizableObject.NAME, keywordName != null ? keywordName : AbstractArtefact.getArtefactName(getArtefactClass()));
        }
    }

    protected String getKeywordName() {
        if (this.standardKeyword != null) {
            return this.standardKeyword;
        } else if (keyword != null && keyword.getKeywordName() != null && !keyword.getKeywordName().isEmpty()) {
            return keyword.getKeywordName();
        } else {
            return null;
        }
    }

    @Override
    protected void fillYamlArtefactFields(CallFunction artefact) {
        super.fillYamlArtefactFields(artefact);
        if (artefact.getArgument() != null) {
            this.inputs = YamlDynamicInputs.fromDynamicValue(artefact.getArgument());
        }
        if (artefact.getFunction() != null) {
            if (artefact.isStandardFunction()) {
                this.standardKeyword = artefact.getFunction().getValue();
            } else {
                this.keyword = YamlKeywordDefinition.fromDynamicValue(artefact.getFunction());
            }
        }

    }

    @Override
    protected String getDefaultNodeNameForYaml(CallFunction artefact) {
        if (artefact.getFunction() != null) {
            String keywordName = YamlKeywordDefinition.fromDynamicValue(artefact.getFunction()).getKeywordName();
            if (keywordName != null && !keywordName.isEmpty()) {
                return keywordName;
            }
        }
        return super.getDefaultNodeNameForYaml(artefact);
    }

    public static class PredefinedKeywordJsonSchemaProcessor implements JsonSchemaFieldProcessor {

        @Override
        public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput, JsonSchemaCreator schemaCreator) throws JsonSchemaPreparationException {
            JsonProvider jsonProvider = JsonProvider.provider();
            JsonArrayBuilder standardKeywordNamesArray = jsonProvider.createArrayBuilder();
            for (String stdKeyword : new StandardKeywordsLookuper().lookupStandardKeywords()) {
                standardKeywordNamesArray.add(stdKeyword);
            }
            propertiesBuilder.add("enum", standardKeywordNamesArray);
            return true;
        }
    }
}
