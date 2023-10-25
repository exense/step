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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.JsonSchemaValidator;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.yaml.deserialization.YamlKeywordDeserializer;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.deserializers.YamlDynamicValueDeserializer;
import step.plans.parser.yaml.YamlPlanReader;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.schema.YamlPlanValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AutomationPackageDescriptorReader {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageDescriptorReader.class);

    private final ObjectMapper yamlObjectMapper;

    private final YamlPlanReader planReader;

    private String jsonSchema;

    public AutomationPackageDescriptorReader() {
        this.planReader = new YamlPlanReader(YamlPlanVersions.ACTUAL_VERSION, null);
        this.yamlObjectMapper = createYamlObjectMapper();

        // TODO: configurable json schema
        this.jsonSchema = readJsonSchema(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH);
    }

    public AutomationPackageDescriptorYaml readAutomationPackageDescriptor(InputStream yamlDescriptor) throws AutomationPackageReadingException {
        log.info("Reading automation package descriptor...");
        return readAutomationPackageYamlFile(yamlDescriptor, AutomationPackageDescriptorYaml.class);
    }

    public AutomationPackageFragmentYaml readAutomationPackageFragment(InputStream yamlFragment, String fragmentName) throws AutomationPackageReadingException {
        log.info("Reading automation package descriptor fragment ({})...", fragmentName);
        return readAutomationPackageYamlFile(yamlFragment, AutomationPackageFragmentYaml.class);
    }

    protected <T extends AutomationPackageFragmentYaml> T readAutomationPackageYamlFile(InputStream yaml, Class<T> targetClass) throws AutomationPackageReadingException {
        try {
            String yamlDescriptorString = new String(yaml.readAllBytes(), StandardCharsets.UTF_8);

            if (jsonSchema != null) {
                try {
                    JsonSchemaValidator.validate(jsonSchema, yamlObjectMapper.readTree(yamlDescriptorString).toString());
                } catch (Exception ex) {
                    throw new YamlPlanValidationException(ex.getMessage(), ex);
                }
            }

            T res = yamlObjectMapper.readValue(yamlDescriptorString, targetClass);

            if (!res.getKeywords().isEmpty()) {
                log.info("{} keyword(s) found in automation package", res.getKeywords().size());
            }
            if (!res.getPlans().isEmpty()) {
                log.info("{} plan(s) found in automation package", res.getPlans().size());
            }
            if (!res.getScheduler().isEmpty()) {
                log.info("{} scheduled task(s) found in automation package", res.getScheduler().size());
            }
            if (res.getFragments().isEmpty()) {
                log.info("{} imported fragment(s) found in automation package", res.getFragments().size());
            }
            return res;
        } catch (IOException | YamlPlanValidationException e) {
            throw new AutomationPackageReadingException("Unable to read the automation package yaml", e);
        }
    }

    protected String readJsonSchema(String jsonSchemaPath) {
        try (InputStream jsonSchemaInputStream = this.getClass().getClassLoader().getResourceAsStream(jsonSchemaPath)) {
            if (jsonSchemaInputStream == null) {
                throw new IllegalStateException("Json schema not found: " + jsonSchemaPath);
            }
            return new String(jsonSchemaInputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load json schema: " + jsonSchemaPath, e);
        }
    }

    protected ObjectMapper createYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();

        // Disable native type id to enable conversion to generic Documents
        yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
        ObjectMapper yamlMapper = DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);

        // configure custom deserializers
        SimpleModule module = new SimpleModule();

        // register serializers/deserializers to read yaml plans
        // TODO: we don't want to use the default upgradable plan serializer, because the plan version is defined via automation package schema version, but not inside the plan...
        planReader.registerAllSerializers(module);

        module.addDeserializer(DynamicValue.class, new YamlDynamicValueDeserializer());
        module.addDeserializer(AutomationPackageKeyword.class, new YamlKeywordDeserializer());

        yamlMapper.registerModule(module);

        return yamlMapper;
    }

    public YamlPlanReader getPlanReader(){
        return this.planReader;
    }
}
