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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.yaml.YamlModelUtils;
import step.core.yaml.serializers.NamedEntityYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;
import step.datapool.DataPoolConfiguration;

import java.io.IOException;

@StepYamlSerializerAddOn(targetClasses = {NamedYamlDataSource.class})
public class NamedYamlDataSourceSerializer extends StepYamlSerializer<NamedYamlDataSource> {

    private static final Logger log = LoggerFactory.getLogger(NamedYamlDataSourceSerializer.class);

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
                return YamlModelUtils.getEntityNameByClass(YamlDataSourceLookuper.resolveDataPool((Class<? extends AbstractYamlDataSource<?>>) value.getClass()));
            }

            @Override
            protected void writeObject(AbstractYamlDataSource<?> value, JsonGenerator gen) throws IOException {
                if (log.isDebugEnabled()) {
                    log.debug("Analyze default values for " + value.getClass().getName());
                }
                //for datasource some "default" values still are relevant and should appear in the generated Yaml
                //we only want to remove default values for the protect and forWrite fields
                DataPoolConfiguration defaultDataPoolConfiguration = new DataSourceDefaultValues();
                ObjectNode defaultJson = yamlObjectMapper.valueToTree(defaultDataPoolConfiguration);
                ObjectNode actualValue = yamlObjectMapper.valueToTree(value);
                removeDefaultValues(actualValue, defaultJson);
                gen.writeObject(actualValue);
            }
        };
        ser.serialize(value.getYamlDataSource(), gen, serializers);
    }

    private static class DataSourceDefaultValues extends DataPoolConfiguration {
    }
}
