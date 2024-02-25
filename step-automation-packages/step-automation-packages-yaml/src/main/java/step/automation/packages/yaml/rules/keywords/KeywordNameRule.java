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
package step.automation.packages.yaml.rules.keywords;

import jakarta.json.spi.JsonProvider;
import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.automation.packages.model.AbstractYamlKeyword;
import step.automation.packages.yaml.rules.YamlConversionRuleAddOn;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.core.accessors.AbstractOrganizableObject;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

import java.lang.reflect.Field;

@YamlConversionRuleAddOn
public class KeywordNameRule implements YamlKeywordConversionRule {

    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
            if (isNameField(field)) {
                // use artefact name as default
                propertiesBuilder.add(
                        AbstractOrganizableObject.NAME,
                        jsonProvider.createObjectBuilder()
                                .add("type", "string")
                                .add("default", AutomationPackageNamedEntityUtils.getEntityNameByClass(objectClass))
                );
                return true;
            } else {
                return false;
            }
        };
    }

    private boolean isNameField(Field field) {
        return field.getDeclaringClass().equals(AbstractYamlKeyword.class) && field.getName().equals("name");
    }

}
