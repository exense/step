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
import org.apache.commons.io.FileUtils;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageWriteToDiskException;
import step.core.yaml.deserialization.AutomationPackageConcurrentEditException;
import step.core.yaml.deserialization.PatchableYamlList;
import step.core.yaml.deserialization.PatchingContext;
import step.plans.automation.YamlPlainTextPlan;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class AbstractAutomationPackageFragmentYaml implements AutomationPackageFragmentYaml {
    private final ObjectMapper mapper;
    private final AutomationPackageSerializationRegistry serializationRegistry;
    private List<String> fragments = new ArrayList<>();
    private List<YamlAutomationPackageKeyword> keywords = new ArrayList<>();
    private PatchableYamlList<YamlPlan> plans;
    private List<YamlPlainTextPlan> plansPlainText = new ArrayList<>();

    private final Map<String, List<?>> additionalFields = new HashMap<>();
    private PatchingContext context;
    private long fileLastModified = 0;

    public AbstractAutomationPackageFragmentYaml(ObjectMapper mapper, AutomationPackageSerializationRegistry serializationRegistry, PatchingContext patchingContext) {
        this.mapper = mapper;
        this.serializationRegistry = serializationRegistry;
        context = patchingContext;
        plans = new PatchableYamlList<>(patchingContext, YamlPlan.PLANS_ENTITY_NAME);
    }

    @JsonIgnore
    private URL url;

    @Override
    public List<YamlAutomationPackageKeyword> getKeywords() {
        return keywords;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setKeywords(PatchableYamlList<YamlAutomationPackageKeyword> keywords) {
        this.keywords = keywords;
    }

    @Override
    public PatchableYamlList<YamlPlan> getPlans() {
        return plans;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setPlans(PatchableYamlList<YamlPlan> plans) {
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
        resetLastModified();
        this.url = url;
    }

    private void resetLastModified() {
        fileLastModified = System.currentTimeMillis();
    }

    @JsonIgnore
    public URL getFragmentUrl() {
        return url;
    }

    @JsonIgnore
    @Override
    public void setPatchingContext(PatchingContext context) {
        this.context = context;
    }

    @JsonIgnore
    @Override
    public PatchingContext getPatchingContext() {
        return context;
    }


    @Override
    public void writeToDisk() {
        try {
            File file = new File(url.toURI());
            if (file.exists() && file.lastModified() > fileLastModified) {
                throw new AutomationPackageConcurrentEditException(MessageFormat.format("Automation package fragment {0} was edited outside the editor.", url));
            }
            FileUtils.writeStringToFile(file, context.getYaml(), StandardCharsets.UTF_8);
            resetLastModified();
        } catch (IOException | URISyntaxException e) {
            throw new AutomationPackageWriteToDiskException(MessageFormat.format("Error when writing automation package fragment {0} back to disk.", url), e);
        }
    }
}
