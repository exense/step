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
package step.parameter.automation;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.*;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.accessors.InMemoryAccessor;
import step.parameter.Parameter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AutomationPackageParameterHook implements AutomationPackageHook<Parameter> {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageParameterHook.class);

    public static final String PARAMETER_ACCESSOR_EXTENSION = "parameterAccessor";

    private final Accessor<Parameter> mainAccessor;

    public AutomationPackageParameterHook(Accessor<Parameter> mainAccessor) {
        this.mainAccessor = mainAccessor;
    }

    @Override
    public void onMainAutomationPackageManagerCreate(Map<String, Object> extensions) {
        extensions.put(PARAMETER_ACCESSOR_EXTENSION, this.mainAccessor);
    }

    @Override
    public void onIsolatedAutomationPackageManagerCreate(Map<String, Object> extensions) {
        extensions.put(PARAMETER_ACCESSOR_EXTENSION, new InMemoryAccessor<>());
    }

    @Override
    public void onPrepareStaging(String fieldName, AutomationPackageContext apContext, AutomationPackageContent apContent, List<?> objects, AutomationPackage oldPackage, AutomationPackageStaging targetStaging) {
        targetStaging.getAdditionalObjects().put(
                AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP,
                objects.stream().map(p -> ((AutomationPackageParameter)p).toParameter()).collect(Collectors.toList())
        );
    }

    @Override
    public void onCreate(List<? extends Parameter> entities, AutomationPackageContext context) {
        for (Parameter entity : entities) {
            // enrich with automation package id
            context.getEnricher().accept(entity);
            getParameterAccessor(context).save(entity);
        }
    }

    @Override
    public void onDelete(AutomationPackage automationPackage, AutomationPackageContext context) {
        List<Parameter> parameters = getParameterAccessor(context).findManyByCriteria(getAutomationPackageIdCriteria(automationPackage.getId())).collect(Collectors.toList());
        for (Parameter parameter : parameters) {
            try {
                getParameterAccessor(context).remove(parameter.getId());
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

    protected Accessor<Parameter> getParameterAccessor(AutomationPackageContext context){
        return (Accessor<Parameter>) context.getExtensions().get(PARAMETER_ACCESSOR_EXTENSION);
    }

}
