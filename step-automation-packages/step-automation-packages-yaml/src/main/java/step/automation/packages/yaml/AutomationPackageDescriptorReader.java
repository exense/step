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
import org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.JsonSchemaValidator;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYamlOS;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlOS;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.yaml.deserializers.StepYamlDeserializersScanner;
import step.plans.parser.yaml.YamlPlanReader;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.schema.YamlPlanValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AutomationPackageDescriptorReader {

    protected static final Logger log = LoggerFactory.getLogger(AutomationPackageDescriptorReader.class);

    protected final ObjectMapper yamlObjectMapper;

    protected final YamlPlanReader planReader;

    protected String jsonSchema;

    public AutomationPackageDescriptorReader(String jsonSchema) {
        // TODO: we need to find a way to resolve the actual json schema (controller config) depending on running server instance (EE or OS)
        // TODO: also we have to resolve the json version for plans according to the automation package version!
        this.planReader = new YamlPlanReader(null, YamlPlanVersions.ACTUAL_VERSION, false, null);
        this.yamlObjectMapper = createYamlObjectMapper();

        if (jsonSchema != null) {
            this.jsonSchema = readJsonSchema(jsonSchema);
        }
    }

    public AutomationPackageDescriptorYaml readAutomationPackageDescriptor(InputStream yamlDescriptor, String packageFileName) throws AutomationPackageReadingException {
        log.info("Reading automation package descriptor...");
        return readAutomationPackageYamlFile(yamlDescriptor, getDescriptorClass(), packageFileName);
    }

    protected Class<? extends AutomationPackageDescriptorYaml> getDescriptorClass() {
        return AutomationPackageDescriptorYamlOS.class;
    }

    public AutomationPackageFragmentYaml readAutomationPackageFragment(InputStream yamlFragment, String fragmentName, String packageFileName) throws AutomationPackageReadingException {
        log.info("Reading automation package descriptor fragment ({})...", fragmentName);
        return readAutomationPackageYamlFile(yamlFragment, getFragmentClass(), packageFileName);
    }

    protected Class<? extends AutomationPackageFragmentYaml> getFragmentClass() {
        return AutomationPackageFragmentYamlOS.class;
    }

    protected <T extends AutomationPackageFragmentYaml> T readAutomationPackageYamlFile(InputStream yaml, Class<T> targetClass, String packageFileName) throws AutomationPackageReadingException {
        try {
            String yamlDescriptorString = new String(yaml.readAllBytes(), StandardCharsets.UTF_8);

            if (jsonSchema != null) {
                try {
                    JsonSchemaValidator.validate(jsonSchema, yamlObjectMapper.readTree(yamlDescriptorString).toString());
                } catch (Exception ex) {
                    // add error details
                    String message = ex.getMessage();
                    if (ex instanceof ValidationException) {
                        message = message + " " + ((ValidationException) ex).getAllMessages();
                    }
                    throw new YamlPlanValidationException(message, ex);
                }
            }

            T res = yamlObjectMapper.readValue(yamlDescriptorString, targetClass);

            logAfterRead(packageFileName, res);
            return res;
        } catch (IOException | YamlPlanValidationException e) {
            throw new AutomationPackageReadingException("Unable to read the automation package yaml. Caused by: " + e.getMessage(), e);
        }
    }

    protected <T extends AutomationPackageFragmentYaml> void logAfterRead(String packageFileName, T res) {
        if (!res.getKeywords().isEmpty()) {
            log.info("{} keyword(s) found in automation package {}", res.getKeywords().size(), StringUtils.defaultString(packageFileName));
        }
        if (!res.getPlans().isEmpty()) {
            log.info("{} plan(s) found in automation package {}", res.getPlans().size(), StringUtils.defaultString(packageFileName));
        }
        if (!res.getSchedules().isEmpty()) {
            log.info("{} schedule(s) found in automation package {}", res.getSchedules().size(), StringUtils.defaultString(packageFileName));
        }
        if (!res.getFragments().isEmpty()) {
            log.info("{} imported fragment(s) found in automation package {}", res.getFragments().size(), StringUtils.defaultString(packageFileName));
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

    public ObjectMapper createYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();

        // Disable native type id to enable conversion to generic Documents
        yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
        ObjectMapper yamlMapper = DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);

        // configure custom deserializers
        SimpleModule module = new SimpleModule();

        // register deserializers to read yaml plans
        planReader.registerAllSerializersAndDeserializers(module, yamlMapper, false);

        yamlMapper.registerModule(module);

        return yamlMapper;
    }


    public YamlPlanReader getPlanReader(){
        return this.planReader;
    }
}
