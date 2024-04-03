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
package step.artefacts.automation.datasource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.core.yaml.serializers.NamedEntityYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;

import java.io.IOException;

@StepYamlSerializerAddOn(targetClasses = {NamedYamlDataSource.class})
public class NamedYamlDataSourceSerializer extends StepYamlSerializer<NamedYamlDataSource> {

    public NamedYamlDataSourceSerializer() {
    }

    public NamedYamlDataSourceSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public void serialize(NamedYamlDataSource value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        NamedEntityYamlSerializer<AbstractYamlDataSource<?>> ser = new NamedEntityYamlSerializer<>() {
            @Override
            protected String resolveYamlName(AbstractYamlDataSource<?> value) {
                return AutomationPackageNamedEntityUtils.getEntityNameByClass(YamlDataSourceLookuper.resolveDataPool((Class<? extends AbstractYamlDataSource<?>>) value.getClass()));
            }

        };
        ser.serialize(value.getYamlDataSource(), gen, serializers);
    }
}
