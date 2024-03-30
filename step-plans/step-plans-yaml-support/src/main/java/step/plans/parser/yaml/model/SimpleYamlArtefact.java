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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.core.artefacts.AbstractArtefact;
import step.plans.parser.yaml.SerializationUtils;

import java.io.IOException;
import java.util.List;

public class SimpleYamlArtefact<T extends AbstractArtefact> extends AbstractYamlArtefact<T> {

    @JsonIgnore
    protected ObjectNode fieldValues;

    public SimpleYamlArtefact(Class<T> techArtefactClass, ObjectNode fieldValues, ObjectMapper yamlObjectMapper) {
        this.artefactClass = techArtefactClass;
        this.fieldValues = fieldValues;
        setYamlObjectMapper(yamlObjectMapper);
    }

    @Override
    protected void fillArtefactFields(T res) {
        super.fillArtefactFields(res);
        try {
            yamlObjectMapper.readerForUpdating(res).readValue(fieldValues);
        } catch (IOException e) {
            throw new RuntimeException("Unable to fill artefact " + artefactClass, e);
        }
    }

    @Override
    protected void fillYamlArtefactFields(T artefact) {
        super.fillYamlArtefactFields(artefact);

        ObjectNode jsonNode = yamlObjectMapper.valueToTree(artefact);
        List<String> basicFieldName = SerializationUtils.getJsonFieldNames(yamlObjectMapper, AbstractArtefact.class);
        for (String s : basicFieldName) {
            jsonNode.remove(s);
        }
        jsonNode.remove(AbstractArtefact.JSON_CLASS_PROPERTY);
        this.fieldValues = jsonNode;
    }

    public ObjectNode toFullJson() {
        ObjectNode jsonNode = yamlObjectMapper.valueToTree(this);
        jsonNode.setAll(fieldValues);
        return jsonNode;
    }
}
