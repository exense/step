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
package step.automation.packages.yaml.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.plans.automation.YamlPlainTextPlan;
import step.plans.parser.yaml.YamlPlan;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public abstract class AbstractAutomationPackageFragmentYaml implements AutomationPackageFragmentYaml {
    private final ObjectMapper mapper;
    private final AutomationPackageSerializationRegistry serializationRegistry;
    private List<String> fragments = new ArrayList<>();
    private List<YamlAutomationPackageKeyword> keywords = new ArrayList<>();
    private List<YamlPlan> plans = new ArrayList<>();
    private List<YamlPlainTextPlan> plansPlainText = new ArrayList<>();

    private final Map<String, List<?>> additionalFields = new HashMap<>();

    @JsonCreator
    public AbstractAutomationPackageFragmentYaml(@JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper mapper, @JacksonInject(useInput = OptBoolean.FALSE) AutomationPackageSerializationRegistry serializationRegistry) {
        this.mapper = mapper;
        this.serializationRegistry = serializationRegistry;
    }
    
    public AbstractAutomationPackageFragmentYaml() {
        this.mapper = null;
        this.serializationRegistry = null;
    };

    @JsonIgnore
    private URL url;
    
    @JsonIgnore
    private String currentYaml;

    @Override
    public List<YamlAutomationPackageKeyword> getKeywords() {
        return keywords;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setKeywords(List<YamlAutomationPackageKeyword> keywords) {
        this.keywords = keywords;
    }

    @Override
    public List<YamlPlan> getPlans() {
        return plans;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setPlans(List<YamlPlan> plans) {
        this.plans = plans;
    }

    @Override
    public List<String> getFragments() {
        return fragments;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setFragments(List<String> fragments) {
        this.fragments = fragments;
    }

    @JsonAnyGetter
    public Map<String, List<?>> getAdditionalFields() {
        return additionalFields;
    }

    @JsonAnySetter
    @Override
    public void setAdditionalFields(String key, JsonNode node) throws IOException {
        if (mapper == null || serializationRegistry == null) return;
            
        // acquire reader for the right type
        Class<?> targetClass = serializationRegistry.resolveClassForYamlField(key);
        if (targetClass == null) return;

        List<?> list = mapper.readerForListOf(targetClass).readValue(node);
        additionalFields.put(key,  list);
    }

    @Override
    public List<YamlPlainTextPlan> getPlansPlainText() {
        return plansPlainText;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setPlansPlainText(List<YamlPlainTextPlan> plansPlainText) {
        this.plansPlainText = plansPlainText;
    }

    @JsonIgnore
    public void setFragmentUrl(URL url) {
        this.url = url;
    }

    @JsonIgnore
    public URL getFragmentUrl() {
        return url;
    }

    @JsonIgnore
    public void setCurrentYaml(String yaml) {
        this.currentYaml = yaml;
    }

    @JsonIgnore
    public String getCurrentYaml() {
        return currentYaml;
    }
}
