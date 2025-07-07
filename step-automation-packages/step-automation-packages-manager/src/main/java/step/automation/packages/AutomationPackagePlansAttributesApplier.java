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
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.core.objectenricher.ObjectEnricher;
import step.core.plans.Plan;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutomationPackagePlansAttributesApplier {

    private static final String STEP_PACKAGE = "step";

    private final ResourceManager resourceManager;
    private final FileResolver fileResolver;

    public AutomationPackagePlansAttributesApplier(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.fileResolver = new FileResolver(resourceManager);
    }

    public void applySpecialAttributesToPlans(List<Plan> plans,
                                              AutomationPackageArchive automationPackageArchive,
                                              AutomationPackageContent packageContent,
                                              String keywordLibraryResourceString,
                                              ObjectEnricher objectEnricher, Map<String, Object> extensions, AutomationPackageOperationMode operationMode) {
        AutomationPackageContext apContext = prepareContext(operationMode, automationPackageArchive, packageContent, keywordLibraryResourceString, objectEnricher, extensions);
        for (Plan plan : plans) {
            applySpecialValuesForArtifact(plan.getRoot(), apContext);
        }
    }

    protected AutomationPackageContext prepareContext(AutomationPackageOperationMode operationMode, AutomationPackageArchive automationPackageArchive, AutomationPackageContent packageContent, String keywordLibraryResourceString, ObjectEnricher enricher, Map<String, Object> extensions) {
        return new AutomationPackageContext(operationMode, resourceManager, automationPackageArchive, packageContent, keywordLibraryResourceString, enricher, extensions);
    }

    private void applySpecialValuesForArtifact(AbstractArtefact artifact, AutomationPackageContext apContext) {
        fillResources(artifact, apContext);
        applySpecialValuesForChildren(artifact, apContext);
    }

    private void fillResources(Object object, AutomationPackageContext apContext) {
        try {
            applyResourcePropertyRecursively(object, apContext);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read yaml artifact " + object.getClass(), e);
        }
    }

    // TODO: some common pluggable approach should be used for keyword resources and plan resources
    private List<PropertyDescriptor> getResourceReferencePropertyDescriptors(Class<?> aClass) {
        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(aClass).getPropertyDescriptors();
            List<PropertyDescriptor> entityReferenceDescriptors = new ArrayList<>();
            for (PropertyDescriptor pd : propertyDescriptors) {
                Method readMethod = pd.getReadMethod();
                if (readMethod != null) {
                    EntityReference entityReference = readMethod.getAnnotation(EntityReference.class);
                    if (entityReference != null && EntityManager.resources.equals(entityReference.type())) {
                        entityReferenceDescriptors.add(pd);
                    }
                }
            }
            return entityReferenceDescriptors;
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyResourcePropertyRecursively(Object object, AutomationPackageContext apContext) throws InvocationTargetException, IllegalAccessException {
        if (object == null) {
            return;
        }

        for (PropertyDescriptor pd : getResourceReferencePropertyDescriptors(object.getClass())) {
            Method setter = pd.getWriteMethod();
            if (setter != null) {
                Object value = pd.getReadMethod().invoke(object);
                if (value instanceof String) {
                    String stringValue = (String) value;
                    if (!fileResolver.isResource(stringValue)) {
                        String uploadedResource = uploadAutomationPackageResource(stringValue, apContext);
                        setter.invoke(object, uploadedResource);
                    }
                } else if (value instanceof DynamicValue) {
                    DynamicValue<String> dynamicValue = (DynamicValue<String>) value;
                    if (dynamicValue.getValue() != null && !fileResolver.isResource(dynamicValue.getValue())) {
                        String uploadedResource = uploadAutomationPackageResource(dynamicValue.getValue(), apContext);
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
            applyResourcePropertyRecursively(nestedField.get(object), apContext);
        }
    }

    private String uploadAutomationPackageResource(String yamlResourceRef, AutomationPackageContext apContext) {
        AutomationPackageResourceUploader resourceUploader = new AutomationPackageResourceUploader();
        Resource resource = resourceUploader.uploadResourceFromAutomationPackage(yamlResourceRef, ResourceManager.RESOURCE_TYPE_FUNCTIONS, apContext);
        String result = null;
        if (resource != null) {
            result = FileResolver.RESOURCE_PREFIX + resource.getId().toString();
        }
        return result;
    }

    private void applySpecialValuesForChildren(AbstractArtefact parent, AutomationPackageContext apContext) {
        List<AbstractArtefact> children = parent.getChildren();
        if (children != null) {
            for (AbstractArtefact child : children) {
                applySpecialValuesForArtifact(child, apContext);
            }
        }
    }

}
