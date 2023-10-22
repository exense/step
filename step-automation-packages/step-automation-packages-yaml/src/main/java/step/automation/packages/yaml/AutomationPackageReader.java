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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageFile;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.yaml.deserialization.YamlKeywordDeserializer;
import step.automation.packages.yaml.model.AutomationPackage;
import step.automation.packages.yaml.model.AutomationPackageKeyword;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.yaml.deserializers.YamlDynamicValueDeserializer;
import step.plans.parser.yaml.YamlPlanReader;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.schema.YamlPlanValidationException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AutomationPackageReader {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageReader.class);

    private final ObjectMapper yamlObjectMapper;
    private final YamlPlanReader planReader;

    public AutomationPackageReader() {
        this.yamlObjectMapper = createYamlObjectMapper();
        this.planReader = new YamlPlanReader(YamlPlanVersions.ACTUAL_VERSION, null);
    }

    public AutomationPackage readAutomationPackage(AutomationPackageFile automationPackageFile) throws AutomationPackageReadingException {
        try {
            if (!automationPackageFile.isAutomationPackage()) {
                return null;
            }

            try (InputStream yamlInputStream = automationPackageFile.getDescriptorYaml()) {
                return readAutomationPackageFromDescriptor(yamlInputStream);
            }
        } catch (IOException ex) {
            throw new AutomationPackageReadingException("Unable to read the automation package", ex);
        }
    }

    public AutomationPackage readAutomationPackageFromJarFile(File automationPackageJar) throws AutomationPackageReadingException {
        return readAutomationPackage(new AutomationPackageFile(automationPackageJar));
    }

    public AutomationPackage readAutomationPackageFromDescriptor(InputStream yamlDescriptor) throws AutomationPackageReadingException {
        try {
            log.info("Reading automation package descriptor...");

            // TODO: validate with json schema
            JsonNode tree = yamlObjectMapper.readTree(yamlDescriptor);

            AutomationPackage res = new AutomationPackage();
            if (tree.has("name")) {
                res.setName(tree.get("name").asText());
            }
            if (tree.has("version")) {
                res.setVersion(tree.get("version").asText());
            }

            if (tree.has("keywords") && tree.get("keywords").isArray()) {
                CollectionType javaType = yamlObjectMapper.getTypeFactory().constructCollectionType(List.class, AutomationPackageKeyword.class);
                List<AutomationPackageKeyword> keywords = yamlObjectMapper.treeToValue(tree.get("keywords"), javaType);
                res.setKeywords(keywords);
            } else {
                res.setKeywords(new ArrayList<>());
            }
            log.info("{} keyword(s) found in automation package", res.getKeywords().size());

            List<Plan> plans = new ArrayList<>();
            if (tree.has("plans") && tree.get("plans").isArray()) {
                for (JsonNode planJsonNode : tree.get("plans")) {
                    try (InputStream is = new ByteArrayInputStream(planJsonNode.toPrettyString().getBytes())) {
                        Plan plan = planReader.readYamlPlan(is);
                        if (plan != null) {
                            plans.add(plan);
                        }
                    }
                }
            }
            res.setPlans(plans);
            log.info("{} plan(s) found in automation package", res.getPlans().size());

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
        module.addDeserializer(DynamicValue.class, new YamlDynamicValueDeserializer());
        module.addDeserializer(AutomationPackageKeyword.class, new YamlKeywordDeserializer());
        yamlMapper.registerModule(module);

        return yamlMapper;
    }
}
