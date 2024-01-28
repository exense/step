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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectEnricher;
import step.core.plans.Plan;
import step.plans.parser.yaml.rules.DataSourceFieldsYamlHelper;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutomationPackagePlansAttributesApplier {

    private static final String STEP_PACKAGE = "step";

    private final ResourceManager resourceManager;

    public AutomationPackagePlansAttributesApplier(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void applySpecialAttributesToPlans(List<Plan> plans,
                                              AutomationPackageArchive automationPackageArchive,
                                              ObjectEnricher objectEnricher) {
        AutomationPackageAttributesApplyingContext apContext = prepareContext(automationPackageArchive, objectEnricher);
        for (Plan plan : plans) {
            applySpecialValuesForArtifact(plan.getRoot(), apContext);
        }
    }

    protected AutomationPackageAttributesApplyingContext prepareContext(AutomationPackageArchive automationPackageArchive, ObjectEnricher enricher) {
        return new AutomationPackageAttributesApplyingContext(resourceManager, automationPackageArchive, enricher);
    }

    private void applySpecialValuesForArtifact(AbstractArtefact artifact, AutomationPackageAttributesApplyingContext apContext) {
        fillResources(artifact, apContext);
        applySpecialValuesForChildren(artifact, apContext);
    }

    // TODO: some common pluggable approach should be used for keyword resources and plan resources
    private void fillResources(Object object, AutomationPackageAttributesApplyingContext apContext) {
        try {
            applyResourcePropertyRecursively(object, apContext);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read yaml artifact " + object.getClass(), e);
        }
    }

    private void applyResourcePropertyRecursively(Object object, AutomationPackageAttributesApplyingContext apContext) throws InvocationTargetException, IllegalAccessException {
        if (object == null) {
            return;
        }

        DataSourceFieldsYamlHelper dsFieldHelper = new DataSourceFieldsYamlHelper();
        for (PropertyDescriptor pd : dsFieldHelper.getResourceReferencePropertyDescriptors(object.getClass())) {
            Method setter = pd.getWriteMethod();
            if (setter != null) {
                Object value = pd.getReadMethod().invoke(object);
                if (value instanceof String) {
                    String stringValue = (String) value;
                    if (!stringValue.startsWith(FileResolver.RESOURCE_PREFIX)) {
                        String uploadedResource = uploadAutomationPackageResource(stringValue, apContext);
                        setter.invoke(object, uploadedResource);
                    }
                } else if (value instanceof DynamicValue) {
                    DynamicValue<String> dynamicValue = (DynamicValue<String>) value;
                    if (dynamicValue.getValue() != null && !dynamicValue.getValue().startsWith(FileResolver.RESOURCE_PREFIX)) {
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

    private String uploadAutomationPackageResource(String yamlResourceRef, AutomationPackageAttributesApplyingContext apContext) {
        AutomationPackageResourceUploader resourceUploader = new AutomationPackageResourceUploader();
        Resource resource = resourceUploader.uploadResourceFromAutomationPackage(yamlResourceRef, ResourceManager.RESOURCE_TYPE_FUNCTIONS, apContext);
        String result = null;
        if (resource != null) {
            result = FileResolver.RESOURCE_PREFIX + resource.getId().toString();
        }
        return result;
    }

    private void applySpecialValuesForChildren(AbstractArtefact parent, AutomationPackageAttributesApplyingContext apContext) {
        List<AbstractArtefact> children = parent.getChildren();
        if (children != null) {
            for (AbstractArtefact child : children) {
                applySpecialValuesForArtifact(child, apContext);
            }
        }
    }

}
