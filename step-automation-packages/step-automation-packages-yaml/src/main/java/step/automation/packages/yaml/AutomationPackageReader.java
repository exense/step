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
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageArchive;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.yaml.deserialization.YamlKeywordDeserializer;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageKeyword;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.deserializers.YamlDynamicValueDeserializer;
import step.plans.parser.yaml.YamlPlanReader;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.schema.YamlPlanValidationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class AutomationPackageReader {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageReader.class);

    private final ObjectMapper yamlObjectMapper;
    private final YamlPlanReader planReader;

    public AutomationPackageReader() {
        this.planReader = new YamlPlanReader(YamlPlanVersions.ACTUAL_VERSION, null);
        this.yamlObjectMapper = createYamlObjectMapper();
    }

    public AutomationPackage readAutomationPackage(AutomationPackageArchive automationPackageArchive) throws AutomationPackageReadingException {
        try {
            if (!automationPackageArchive.isAutomationPackage()) {
                return null;
            }

            try (InputStream yamlInputStream = automationPackageArchive.getDescriptorYaml()) {
                AutomationPackageDescriptorYaml descriptorYaml = readAutomationPackageDescriptor(yamlInputStream);
                return buildAutomationPackage(descriptorYaml, automationPackageArchive);
            }
        } catch (IOException ex) {
            throw new AutomationPackageReadingException("Unable to read the automation package", ex);
        }
    }

    protected AutomationPackage buildAutomationPackage(AutomationPackageDescriptorYaml descriptor, AutomationPackageArchive archive){
        // TODO: merge imports
        AutomationPackage res = new AutomationPackage();
        res.setName(descriptor.getName());
        res.setPlans(descriptor.getPlans().stream().map(planReader::yamlPlanToPlan).collect(Collectors.toList()));
        res.setKeywords(descriptor.getKeywords());
        importIntoAutomationPackage(res, descriptor.getImports(), archive);
        return res;
    }

    public void importIntoAutomationPackage(AutomationPackage targetPackage, List<String> imports, AutomationPackageArchive archive){
        // TODO: implement
    }

    public AutomationPackage readAutomationPackageFromJarFile(File automationPackageJar) throws AutomationPackageReadingException {
        return readAutomationPackage(new AutomationPackageArchive(automationPackageJar));
    }

    public AutomationPackageDescriptorYaml readAutomationPackageDescriptor(InputStream yamlDescriptor) throws AutomationPackageReadingException {
        // TODO: validate with json schema
        try {
            log.info("Reading automation package descriptor...");
            AutomationPackageDescriptorYaml res = yamlObjectMapper.readValue(yamlDescriptor, AutomationPackageDescriptorYaml.class);

            log.info("{} keyword(s) found in automation package", res.getKeywords().size());
            log.info("{} plan(s) found in automation package", res.getPlans().size());
            log.info("{} scheduled task(s) found in automation package", res.getScheduler().size());
            return res;
        } catch (IOException | YamlPlanValidationException e) {
            throw new AutomationPackageReadingException("Unable to read the automation package", e);
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
        planReader.registerAllSerializers(module);

        module.addDeserializer(DynamicValue.class, new YamlDynamicValueDeserializer());
        module.addDeserializer(AutomationPackageKeyword.class, new YamlKeywordDeserializer());

        yamlMapper.registerModule(module);

        return yamlMapper;
    }
}
