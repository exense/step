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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.apache.commons.io.FileUtils;
import step.automation.packages.mappers.interfaces.HasCollectionName;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapper;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageWriteToDiskException;
import step.core.accessors.AbstractOrganizableObject;
import step.core.yaml.PatchableYamlModel;
import step.core.yaml.PatchingContext;
import step.core.yaml.deserialization.AutomationPackageConcurrentEditException;
import step.core.yaml.deserialization.PatchableYamlList;
import step.core.yaml.deserialization.PatchableYamlPrimitive;
import step.plans.automation.AutomationPackagePlainTextPlanJsonSchema;
import step.plans.automation.YamlPlainTextPlan;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class AbstractAutomationPackageFragmentYaml implements AutomationPackageFragmentYaml {
    private PatchableYamlList<PatchableYamlPrimitive<String>> fragments;
    private PatchableYamlList<YamlAutomationPackageKeyword> keywords;
    private PatchableYamlList<YamlPlan> plans;
    private PatchableYamlList<YamlPlainTextPlan> plansPlainText;

    private final Map<String, PatchableYamlList<?>> additionalFields = new HashMap<>();
    private PatchingContext context;
    private long fileLastModified = 0;

    public AbstractAutomationPackageFragmentYaml(PatchingContext patchingContext) {
        context = patchingContext;
        plans = new PatchableYamlList<>(patchingContext, YamlPlan.PLANS_ENTITY_NAME);
        keywords = new PatchableYamlList<>(patchingContext, YamlAutomationPackageKeyword.KEYWORDS_ENTITY_NAME);
        plansPlainText = new PatchableYamlList<>(patchingContext, AutomationPackagePlainTextPlanJsonSchema.FIELD_NAME_IN_AP);
        fragments = new PatchableYamlList<>(patchingContext, "fragments");
    }

    @JsonIgnore
    private Path path;

    @Override
    public PatchableYamlList<YamlAutomationPackageKeyword> getKeywords() {
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
    public PatchableYamlList<PatchableYamlPrimitive<String>> getFragments() {
        return fragments;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setFragments(PatchableYamlList<PatchableYamlPrimitive<String>> fragments) {
        this.fragments = fragments;
    }

    @JsonAnyGetter
    @Override
    public Map<String, PatchableYamlList<?>> getAdditionalFields() {
        return additionalFields;
    }

    @Override
    public void setAdditionalFields(String key, PatchableYamlList<?> list) {
        additionalFields.put(key, list);
    }

    @Override
    public PatchableYamlList<YamlPlainTextPlan> getPlansPlainText() {
        return plansPlainText;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setPlansPlainText(PatchableYamlList<YamlPlainTextPlan> plansPlainText) {
        this.plansPlainText = plansPlainText;
    }

    @JsonIgnore
    public void setFragmentPath(Path path) {
        resetLastModified();
        this.path = path;
    }

    private void resetLastModified() {
        fileLastModified = System.currentTimeMillis();
    }

    @JsonIgnore
    public Path getFragmentPath() {
        return path;
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
            File file = path.toFile();
            if (file.exists() && file.lastModified() > fileLastModified) {
                throw new AutomationPackageConcurrentEditException(MessageFormat.format("Automation package fragment {0} was edited outside the editor.", path));
            }
            FileUtils.writeStringToFile(file, context.getCurrentYaml(), StandardCharsets.UTF_8);
            resetLastModified();
        } catch (IOException e) {
            throw new AutomationPackageWriteToDiskException(MessageFormat.format("Error when writing automation package fragment {0} back to disk.", path), e);
        }
    }

    @Override
    public boolean isEmpty() {
        return getFragments().isEmpty() &&
            getPlans().isEmpty() &&
            getPlansPlainText().isEmpty() &&
            getKeywords().isEmpty() &&
            getAdditionalFields().isEmpty();
    }

    @Override
    public <YO extends PatchableYamlModel, BO extends AbstractOrganizableObject> void initializeMaps(YamlToBusinessObjectMapper<YO, BO> mapper, Map<AbstractOrganizableObject, PatchableYamlModel> patchableMap, Map<AbstractOrganizableObject, AutomationPackageFragmentYaml> fragmentMap) {
        for (YO yamlObject : getListForYamlObject(mapper)) {
            BO businessObject = mapper.toBusinessObject(yamlObject);
            patchableMap.put(businessObject, yamlObject);
            fragmentMap.put(businessObject, this);
        }
    }

    @Override
    public <YO extends PatchableYamlModel, BO extends AbstractOrganizableObject> PatchableYamlList<YO> getListForYamlObject(HasCollectionName<BO, YO> mapper) {
        String collectionName = mapper.getCollectionName();
        return (PatchableYamlList<YO>) switch (collectionName) {
            case YamlAutomationPackageKeyword.KEYWORDS_ENTITY_NAME -> keywords;
            case YamlPlan.PLANS_ENTITY_NAME -> plans;
            case AutomationPackagePlainTextPlanJsonSchema.FIELD_NAME_IN_AP -> plansPlainText;
            default -> additionalFields
                .computeIfAbsent(collectionName, n -> new PatchableYamlList<YO>(getPatchingContext(), n));
        };
    }
}
