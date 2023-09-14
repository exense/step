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
package step.automation.packages.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import step.automation.packages.AutomationPackageFile;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.yaml.deserialization.YamlKeywordsDeserializer;
import step.automation.packages.yaml.model.AutomationPackageDescriptor;
import step.automation.packages.yaml.model.AutomationPackageKeyword;
import step.automation.packages.yaml.model.AutomationPackageKeywords;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.deserializers.YamlDynamicValueDeserializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AutomationPackageKeywordsExtractor {

    private final ObjectMapper yamlObjectMapper;

    public AutomationPackageKeywordsExtractor() {
        this.yamlObjectMapper = createYamlObjectMapper();
    }

    public List<AutomationPackageKeyword> extractKeywordsFromAutomationPackage(AutomationPackageFile automationPackageFile) throws AutomationPackageReadingException {
        try {
            if (!automationPackageFile.isAutomationPackage()) {
                return new ArrayList<>();
            }

            try (InputStream yamlInputStream = automationPackageFile.getDescriptorYaml()) {
                return extractKeywordsFromDescriptor(yamlInputStream);
            }
        } catch (IOException ex) {
            throw new AutomationPackageReadingException("Unable to read the automation package", ex);
        }
    }

    List<AutomationPackageKeyword> extractKeywordsFromDescriptor(InputStream yamlDescriptor) throws IOException {
        AutomationPackageDescriptor metadata = yamlObjectMapper.readValue(yamlDescriptor, AutomationPackageDescriptor.class);
        return metadata.getKeywords().getKeywords();
    }

    public List<AutomationPackageKeyword> extractKeywordsFromAutomationPackage(File automationPackageJar) throws AutomationPackageReadingException {
        return extractKeywordsFromAutomationPackage(new AutomationPackageFile(automationPackageJar));
    }

    protected ObjectMapper createYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();

        // Disable native type id to enable conversion to generic Documents
        yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
        ObjectMapper yamlMapper = DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);

        // configure custom deserializers
        SimpleModule module = new SimpleModule();
        module.addDeserializer(DynamicValue.class, new YamlDynamicValueDeserializer());
        module.addDeserializer(AutomationPackageKeywords.class, new YamlKeywordsDeserializer());
        yamlMapper.registerModule(module);

        return yamlMapper;
    }
}
