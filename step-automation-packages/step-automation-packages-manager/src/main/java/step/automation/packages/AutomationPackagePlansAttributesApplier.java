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
package step.automation.packages;

import step.attachments.FileResolver;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.plans.parser.yaml.rules.DataSourceFieldsYamlHelper;
import step.resources.ResourceManager;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutomationPackagePlansAttributesApplier {

    private static final String STEP_PACKAGE = "step";

    private final FileResolver fileResolver;
    private final AutomationPackageResourceUploader resourceUploader;

    public AutomationPackagePlansAttributesApplier(ResourceManager stagingResourceManager) {
        this.fileResolver = new FileResolver(stagingResourceManager);
        this.resourceUploader = new AutomationPackageResourceUploader();
    }

    public void applySpecialAttributesToPlans(List<Plan> plans,
                                              AutomationPackageAttributesApplyingContext context) {
        for (Plan plan : plans) {
            Plan oldPlan = context.getOldPlan(plan.getId());
            applySpecialValuesForArtifact(plan.getRoot(), oldPlan == null ? null : oldPlan.getRoot(), context);
        }
    }

    private void applySpecialValuesForArtifact(AbstractArtefact artifact, AbstractArtefact oldArtifact, AutomationPackageAttributesApplyingContext apContext) {
        fillResources(artifact, oldArtifact, apContext);
        applySpecialValuesForChildren(artifact, oldArtifact, apContext);
    }

    // TODO: some common pluggable approach should be used for keyword resources and plan resources
    private void fillResources(Object object, Object oldObject, AutomationPackageAttributesApplyingContext apContext) {
        try {
            applyResourcePropertyRecursively(object, oldObject, apContext);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read yaml artifact " + object.getClass(), e);
        }
    }

    private void applyResourcePropertyRecursively(Object object, Object oldObject, AutomationPackageAttributesApplyingContext apContext) throws InvocationTargetException, IllegalAccessException {
        if (object == null) {
            return;
        }

        DataSourceFieldsYamlHelper dsFieldHelper = new DataSourceFieldsYamlHelper();
        for (PropertyDescriptor pd : dsFieldHelper.getResourceReferencePropertyDescriptors(object.getClass())) {
            Method setter = pd.getWriteMethod();
            if (setter != null) {
                Object value = pd.getReadMethod().invoke(object);
                Object oldValue = null;
                if (oldObject != null) {
                    try {
                        oldValue = pd.getReadMethod().invoke(oldObject);
                    } catch (Exception ex) {
                        // old object doesn't match - just skip it
                    }
                }

                if (value instanceof String) {
                    String stringValue = (String) value;
                    if (!fileResolver.isResource(stringValue)) {
                        String uploadedResource = uploadAutomationPackageResource(stringValue, apContext, oldValue);
                        setter.invoke(object, uploadedResource);
                    }
                } else if (value instanceof DynamicValue) {
                    DynamicValue<String> dynamicValue = (DynamicValue<String>) value;
                    if (dynamicValue.getValue() != null && !fileResolver.isResource(dynamicValue.getValue())) {
                        String uploadedResource = uploadAutomationPackageResource(dynamicValue.getValue(), apContext, oldValue);
                        setter.invoke(object, new DynamicValue<>(uploadedResource));
                    }
                } else {
                    throw new RuntimeException("Unsupported field type: " + value.getClass());
                }
            } else {
                throw new RuntimeException("Resource reference cannot be applied to " + pd.getName() + " in " + object.getClass().getName() + ". Setter doesn't exist");
            }
        }

        // analyze the class hierarchy
        List<Field> allFieldsInHierarchy = new ArrayList<>();
        Class<?> currentClass = object.getClass();
        while (currentClass != null && currentClass.getPackageName().startsWith(STEP_PACKAGE + ".")) {
            allFieldsInHierarchy.addAll(Stream.of(currentClass.getDeclaredFields())
                    .filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                    .filter(f -> f.getType().getPackageName().startsWith(STEP_PACKAGE + "."))
                    .collect(Collectors.toList()));
            currentClass = currentClass.getSuperclass();
        }

        for (Field nestedField : allFieldsInHierarchy) {
            nestedField.setAccessible(true);
            Object nestedOldObject = null;
            if (oldObject != null) {
                try {
                    nestedOldObject = nestedField.get(oldObject);
                } catch (Exception ex) {
                    // nested old object is not accessible - just skip it
                }
            }
            applyResourcePropertyRecursively(nestedField.get(object), nestedOldObject, apContext);
        }
    }

    private String uploadAutomationPackageResource(String yamlResourceRef, AutomationPackageAttributesApplyingContext apContext, Object oldResourceReference) {
        DynamicValue<String> dynOldResourceReference = null;
        if (oldResourceReference instanceof DynamicValue) {
            dynOldResourceReference = (DynamicValue<String>) oldResourceReference;
        } else if (oldResourceReference instanceof String) {
            dynOldResourceReference = new DynamicValue<>((String) oldResourceReference);
        }
        return resourceUploader.applyResourceReference(yamlResourceRef, ResourceManager.RESOURCE_TYPE_FUNCTIONS, apContext, dynOldResourceReference);
    }

    private void applySpecialValuesForChildren(AbstractArtefact parent, AbstractArtefact oldParent, AutomationPackageAttributesApplyingContext apContext) {
        List<AbstractArtefact> children = parent.getChildren();
        if (children != null) {
            int i = 0;
            for (AbstractArtefact child : children) {

                // TODO: how to find old artefact properly?
                AbstractArtefact oldChild = null;
                if (oldParent != null && oldParent.getChildren().size() > i) {
                    AbstractArtefact potentialChild = oldParent.getChildren().get(i);
                    if (potentialChild.getClass().equals(child.getClass()) && Objects.equals(potentialChild.getAttribute(AbstractOrganizableObject.NAME), child.getAttribute(AbstractOrganizableObject.NAME))) {
                        oldChild = potentialChild;
                    }
                }

                applySpecialValuesForArtifact(child, oldChild, apContext);
                i++;
            }
        }
    }

}
