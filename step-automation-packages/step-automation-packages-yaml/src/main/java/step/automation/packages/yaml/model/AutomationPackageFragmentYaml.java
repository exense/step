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

import com.fasterxml.jackson.annotation.JsonIgnore;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapper;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.core.accessors.AbstractOrganizableObject;
import step.core.yaml.PatchableYamlModel;
import step.core.yaml.PatchingContext;
import step.core.yaml.deserialization.PatchableYamlList;
import step.core.yaml.deserialization.PatchableYamlPrimitive;
import step.core.yaml.deserialization.PatchableYamlScalarField;
import step.plans.automation.YamlPlainTextPlan;
import step.plans.parser.yaml.YamlPlan;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface AutomationPackageFragmentYaml {
    // this name should be kept untouched to support the migrations for old versions
    String VERSION_FIELD_NAME = "version";

    /**
     * @return the schema version declared by this descriptor or fragment. A fragment usually declares none and follows
     * the one of the descriptor or fragment importing it
     */
    PatchableYamlScalarField<String> getVersion();

    void setVersion(PatchableYamlScalarField<String> version);

    @JsonIgnore
    void setVersionString(String version);

    /**
     * @return the schema version this descriptor or fragment was read against: the one it declares, otherwise the one
     * it inherits. Null when neither declares one, the file is then considered as current
     */
    @JsonIgnore
    String getEffectiveVersion();

    @JsonIgnore
    void setEffectiveVersion(String effectiveVersion);

    PatchableYamlList<YamlAutomationPackageKeyword> getKeywords();

    PatchableYamlList<YamlPlan> getPlans();

    List<YamlPlainTextPlan> getPlansPlainText();

    PatchableYamlList<PatchableYamlPrimitive<String>> getFragments();

    /**
     * @return the free-form metadata declared at the top level of this descriptor or fragment, null if there is none
     */
    Map<String, Object> getMetadata();

    Map<String, PatchableYamlList<?>> getAdditionalFields();

    default <T> PatchableYamlList<T> getAdditionalField(String k) {
        return (PatchableYamlList<T>) getAdditionalFields().get(k);
    }

    void setAdditionalFields(String key, PatchableYamlList<?> value) throws IOException;

    Path getFragmentPath();

    void setFragmentPath(Path url);

    PatchingContext getPatchingContext();

    void setPatchingContext(PatchingContext context);

    void writeToDisk();

    boolean isEmpty();

    <YO extends PatchableYamlModel, BO extends AbstractOrganizableObject> void initializeMaps(YamlToBusinessObjectMapper<YO, BO> mapper, Map<AbstractOrganizableObject, PatchableYamlModel> patchableMap, Map<AbstractOrganizableObject, AutomationPackageFragmentYaml> fragmentMap);

    <YO extends PatchableYamlModel> PatchableYamlList<YO> getListForYamlObject(String collectionName);
}
