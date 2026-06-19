package step.automation.packages.yaml.mappers;

import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.accessors.AbstractOrganizableObject;
import step.core.yaml.PatchableYamlModel;
import step.core.yaml.deserialization.PatchableYamlList;
import step.plans.parser.yaml.YamlPlanReader;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
public abstract class ObjectToYamlObjectMapper<BO extends AbstractOrganizableObject, YO extends PatchableYamlModel> {

    public abstract YO getNewYamlObject(BO object);

    public abstract Optional<BO> getBusinessObject(YO yamlModel);

    public abstract PatchableYamlList<YO> getListInFragment(AutomationPackageFragmentYaml fragment);

    public void initializeMaps(AutomationPackageFragmentYaml fragment, Map<AbstractOrganizableObject, PatchableYamlModel> patchableMap, Map<AbstractOrganizableObject, AutomationPackageFragmentYaml> fragmentMap) {
        for (YO yamlObject : getListInFragment(fragment)) {
            try {
                getBusinessObject(yamlObject).ifPresent(object -> {
                    patchableMap.put(object, yamlObject);
                    fragmentMap.put(object, fragment);
                });
            } catch (Exception e) {

                /* TODO: requires proper handling of entities which could not be properly loaded.
                 */
                System.out.println(e);
            }
        }
    }

    public abstract String getCollectionName();
}
