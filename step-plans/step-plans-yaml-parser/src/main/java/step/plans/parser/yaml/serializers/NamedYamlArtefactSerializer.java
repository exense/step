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
package step.plans.parser.yaml.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.core.yaml.serializers.NamedEntityYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;
import step.plans.parser.yaml.model.AbstractYamlArtefact;
import step.plans.parser.yaml.model.NamedYamlArtefact;
import step.plans.parser.yaml.model.SimpleYamlArtefact;

import java.io.IOException;

@StepYamlSerializerAddOn(targetClasses = {NamedYamlArtefact.class})
public class NamedYamlArtefactSerializer extends StepYamlSerializer<NamedYamlArtefact> {

    public NamedYamlArtefactSerializer() {
        super();
    }

    public NamedYamlArtefactSerializer(ObjectMapper stepYamlMapper) {
        super(stepYamlMapper);
    }

    @Override
    public void serialize(NamedYamlArtefact value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        NamedEntityYamlSerializer<AbstractYamlArtefact<?>> ser = new NamedEntityYamlSerializer<>() {
            @Override
            protected String resolveYamlName(AbstractYamlArtefact<?> value) {
                return AutomationPackageNamedEntityUtils.getEntityNameByClass(value.getArtefactClass());
            }

            @Override
            protected void writeObject(AbstractYamlArtefact<?> value, JsonGenerator gen) throws IOException {
                if (value instanceof SimpleYamlArtefact<?>) {
                    gen.writeTree(((SimpleYamlArtefact<?>) value).toFullJson());
                } else {
                    super.writeObject(value, gen);
                }
            }
        };
        ser.serialize(value.getAbstractArtefact(), gen, serializers);
    }

}
