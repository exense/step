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
package step.automation.packages.hooks;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageContext;
import step.automation.packages.AutomationPackageEntity;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.model.AutomationPackageContent;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.objectenricher.ObjectEnricher;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParameterJsonSchema;
import step.parameter.automation.AutomationPackageParameter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AutomationPackageParameterHook implements AutomationPackageHook<Parameter> {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageParameterHook.class);

    private final Accessor<Parameter> accessor;

    public AutomationPackageParameterHook(Accessor<Parameter> accessor) {
        this.accessor = accessor;
    }

    @Override
    public void onPrepareStaging(String fieldName, AutomationPackageContext apContext, AutomationPackageContent apContent, List<?> objects, AutomationPackage oldPackage, AutomationPackageManager.Staging targetStaging, AutomationPackageManager manager) {
        targetStaging.getAdditionalObjects().put(
                AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP,
                objects.stream().map(p -> ((AutomationPackageParameter)p).toParameter()).collect(Collectors.toList())
        );
    }

    @Override
    public void onCreate(List<? extends Parameter> entities, ObjectEnricher enricher, AutomationPackageManager manager) {
        for (Parameter entity : entities) {
            // enrich with automation package id
            enricher.accept(entity);
            accessor.save(entity);
        }
    }

    @Override
    public void onDelete(AutomationPackage automationPackage, AutomationPackageManager manager) {
        List<Parameter> parameters = accessor.findManyByCriteria(getAutomationPackageIdCriteria(automationPackage.getId())).collect(Collectors.toList());
        for (Parameter parameter : parameters) {
            try {
                accessor.remove(parameter.getId());
            } catch (Exception e){
                 log.error("The automation package parameter {} cannot be deleted for automation package {}.", parameter.getId(), automationPackage.getAttribute(AbstractOrganizableObject.NAME), e);
            }
        }
    }

    protected Map<String, String> getAutomationPackageIdCriteria(ObjectId automationPackageId) {
        return Map.of(getAutomationPackageTrackingField(), automationPackageId.toString());
    }

    protected String getAutomationPackageTrackingField() {
        return "customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID;
    }

}
