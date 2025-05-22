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
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Accessor;
import step.core.accessors.InMemoryAccessor;
import step.core.accessors.LayeredAccessor;
import step.core.repositories.ImportResult;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.encryption.EncryptedValueManagerException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AutomationPackageParameterHook implements AutomationPackageHook<Parameter> {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageParameterHook.class);

    public static final String PARAMETER_MANAGER_EXTENSION = "parameterManager";

    private final ParameterManager parameterManager;

    public AutomationPackageParameterHook(ParameterManager parameterManager) {
        this.parameterManager = parameterManager;
    }

    @Override
    public void onMainAutomationPackageManagerCreate(Map<String, Object> extensions) {
        extensions.put(PARAMETER_MANAGER_EXTENSION, this.parameterManager);
    }

    @Override
    public void onIsolatedAutomationPackageManagerCreate(Map<String, Object> extensions) {
        extensions.put(PARAMETER_MANAGER_EXTENSION, ParameterManager.copy(this.parameterManager, new InMemoryAccessor<>()));
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
            try {
                getParameterManager(context.getExtensions()).save(entity, null, null, context.getValidator());
            } catch (EncryptedValueManagerException e) {
                log.error("The automation package parameter {} cannot be saved for automation package {}.", entity.getKey(), context.getAutomationPackageArchive().getOriginalFileName(), e);
                throw new EncryptedValueManagerException("The automation package parameter " + entity.getKey() + " cannot be saved: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDelete(AutomationPackage automationPackage, AutomationPackageContext context) {
        ObjectId apId = automationPackage.getId();
        List<Parameter> parameters = getParametersForAutomationPackage(context, apId);
        for (Parameter parameter : parameters) {
            try {
                getParameterManager(context.getExtensions()).getParameterAccessor().remove(parameter.getId());
            } catch (Exception e){
                 log.error("The automation package parameter {} cannot be deleted for automation package {}.", parameter.getKey(), automationPackage.getAttribute(AbstractOrganizableObject.NAME), e);
            }
        }
    }

    @Override
    public void beforeIsolatedExecution(AutomationPackage automationPackage, AbstractStepContext executionContext, Map<String, Object> apManagerExtensions, ImportResult importResult) {
        ParameterManager apParameterManager = getParameterManager(apManagerExtensions);
        if (apParameterManager != null) {

            // automation package has its own in-memory accessor for parsed parameters - these parameters should be merged
            // with other parameters prepared in execution context
            ParameterManager contextParameterManager = executionContext.get(ParameterManager.class);
            if (contextParameterManager != null) {
                if (!isLayeredAccessor(contextParameterManager.getParameterAccessor())) {
                    importResult.setErrors(List.of(contextParameterManager.getParameterAccessor().getClass() + " is not layered"));
                }
                Iterator<Parameter> iterator = apParameterManager.getParameterAccessor().getAll();
                while (iterator.hasNext()) {
                    Parameter next = iterator.next();
                    contextParameterManager.getParameterAccessor().save(next);
                }
            }
        }
    }

    @Override
    public Map<String, List<? extends AbstractOrganizableObject>> getEntitiesForAutomationPackage(ObjectId automationPackageId, AutomationPackageContext automationPackageContext) {
        return Map.of(AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP, getParametersForAutomationPackage(automationPackageContext, automationPackageId));
    }

    private boolean isLayeredAccessor(Accessor<?> accessor) {
        return accessor instanceof LayeredAccessor;
    }

    protected Map<String, String> getAutomationPackageIdCriteria(ObjectId automationPackageId) {
        return Map.of(getAutomationPackageTrackingField(), automationPackageId.toString());
    }

    protected String getAutomationPackageTrackingField() {
        return "customFields." + AutomationPackageEntity.AUTOMATION_PACKAGE_ID;
    }

    protected ParameterManager getParameterManager(Map<String, Object> extensions){
        return (ParameterManager) extensions.get(PARAMETER_MANAGER_EXTENSION);
    }

    protected List<Parameter> getParametersForAutomationPackage(AutomationPackageContext context, ObjectId apId) {
        return getParameterManager(context.getExtensions()).getParameterAccessor().findManyByCriteria(getAutomationPackageIdCriteria(apId)).collect(Collectors.toList());
    }


}
