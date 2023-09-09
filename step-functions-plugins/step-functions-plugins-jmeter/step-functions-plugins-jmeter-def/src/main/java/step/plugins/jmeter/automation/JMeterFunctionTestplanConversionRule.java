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
package step.plugins.jmeter.automation;

import step.automation.packages.yaml.deserialization.SpecialKeywordAttributesApplier;
import step.automation.packages.yaml.deserialization.SpecialKeywordAttributesExtractor;
import step.automation.packages.yaml.deserialization.YamlKeywordFieldDeserializationProcessor;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.automation.packages.yaml.rules.YamlKeywordConversionRuleMarker;
import step.core.AbstractStepContext;
import step.core.dynamicbeans.DynamicValue;
import step.plugins.jmeter.JMeterFunction;
import step.resources.Resource;
import step.resources.ResourceManager;

@YamlKeywordConversionRuleMarker(functions = JMeterFunction.class)
public class JMeterFunctionTestplanConversionRule implements YamlKeywordConversionRule {

    public static final String JMETER_TESTPLAN_ATTR = "jmeterTestplan";

    @Override
    public SpecialKeywordAttributesExtractor getSpecialAttributesExtractor() {
        return (yamlKeyword, draftKeywordObject, specialAttributesCollector) -> specialAttributesCollector.put(JMETER_TESTPLAN_ATTR, yamlKeyword.get(JMETER_TESTPLAN_ATTR).asText());
    }

    @Override
    public SpecialKeywordAttributesApplier getSpecialKeywordAttributesApplier(AbstractStepContext context) {
        return (keyword, automationPackageFile) -> {
            JMeterFunction draftKeyword = (JMeterFunction) keyword.getDraftKeyword();
            String testplanPath = (String) keyword.getSpecialAttributes().get(JMETER_TESTPLAN_ATTR);
            if (testplanPath != null && !testplanPath.isEmpty()) {
                ResourceManager resourceManager = context.getResourceManager();

                try {
                    Resource resource = resourceManager.createResource(
                            ResourceManager.RESOURCE_TYPE_FUNCTIONS,
                            automationPackageFile.getResourceAsStream(testplanPath),
                            automationPackageFile.getResource(testplanPath).getFile(),
                            false, null
                    );
                    draftKeyword.setJmeterTestplan(new DynamicValue<>(resource.getId().toString()));
                } catch (Exception e) {
                    throw new RuntimeException("Unable to apply jmeter testplan", e);
                }
            }
        };
    }

    @Override
    public YamlKeywordFieldDeserializationProcessor getDeserializationProcessor() {
        return (keywordClass, field, output, codec) -> {
            // TODO: classname or yaml classname?
            if (keywordClass.equalsIgnoreCase(JMeterFunction.class.getName()) && field.getKey().equals(JMETER_TESTPLAN_ATTR)) {
                // ignore the jmeterTestplan attribute - it will be applied via SpecialKeywordAttributesExtractor
                return true;
            } else {
                return false;
            }
        };
    }
}
