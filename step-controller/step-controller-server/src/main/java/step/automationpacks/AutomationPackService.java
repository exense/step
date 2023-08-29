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
package step.automationpacks;

import ch.exense.commons.io.FileHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import step.automationpacks.handlers.AutomationPackKeywordHandler;
import step.automationpacks.model.AbstractAutomationPackKeyword;
import step.automationpacks.model.AutomationPackMetadata;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.deserializers.YamlDynamicValueDeserializer;
import step.core.yaml.serializers.YamlDynamicValueSerializer;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class AutomationPackService {

    private final ObjectMapper yamlMapper;

    private final FunctionAccessor functionAccessor;

    public AutomationPackService(FunctionAccessor functionAccessor) {
        this.yamlMapper = createYamlObjectMapper();
        this.functionAccessor = functionAccessor;
    }

    public String uploadNewPackage(InputStream archive) throws AutomationPackPreparationException {
        try {
            File unzippedFile = FileHelper.unzip(archive, UUID.randomUUID().toString());
            File[] metadataFile = unzippedFile.listFiles((dir, name) -> name.equalsIgnoreCase("metadataFile.yml") || name.equalsIgnoreCase("metadataFile.yaml"));
            if (metadataFile == null || metadataFile.length == 0) {
                throw new AutomationPackPreparationException("Automation package doesn't contain metadataFile file");
            }
            File yamlMetadata = metadataFile[0];

            try (FileInputStream fis = new FileInputStream(yamlMetadata)){
                AutomationPackMetadata metadata = yamlMapper.readValue(fis, AutomationPackMetadata.class);

                // process keywords
                for (AbstractAutomationPackKeyword keyword : metadata.getKeywords()) {
                    AutomationPackKeywordHandler<?> handler = getHandler(keyword);
                    Function function = handler.prepareKeyword(keyword, unzippedFile);
                    functionAccessor.save(function);
                }
            }
        } catch (IOException e) {
            throw new AutomationPackPreparationException("Unable to read automation package archive", e);
        }
        // TODO: create automation package in db?
        return UUID.randomUUID().toString();
    }

    private AutomationPackKeywordHandler<?> getHandler(AbstractAutomationPackKeyword keyword){
        // TODO: resolve handler via annotation
        return new AutomationPackKeywordHandler<Function>() {
            @Override
            public Function prepareKeyword(AbstractAutomationPackKeyword automationPackKeyword, File automationPack) {
                return null;
            }
        };
    }

    protected ObjectMapper createYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();

        // Disable native type id to enable conversion to generic Documents
        yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
        ObjectMapper yamlMapper = DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);

        // configure custom deserializers
        SimpleModule module = new SimpleModule();
        module.addDeserializer(DynamicValue.class, new YamlDynamicValueDeserializer());

        module.addSerializer(DynamicValue.class, new YamlDynamicValueSerializer());
        yamlMapper.registerModule(module);
        return yamlMapper;
    }
}
