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
package step.plans.parser.yaml;

import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.plans.parser.yaml.model.AbstractYamlArtefact;
import step.plans.parser.yaml.model.YamlArtefact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class YamlArtefactsLookuper {

    private static final Map<Class<? extends AbstractYamlArtefact<?>>, Class<? extends AbstractArtefact>> MODEL_TO_ARTEFACT_MAP = createModelToArtefactMap();

    private static Map<Class<? extends AbstractYamlArtefact<?>>, Class<? extends AbstractArtefact>> createModelToArtefactMap() {
        Map<Class<? extends AbstractYamlArtefact<?>>, Class<? extends AbstractArtefact>> res = new HashMap<>();
        List<Class<? extends AbstractArtefact>> artefactsWithSpecialModels = getArtefactsWithSpecialModels();
        artefactsWithSpecialModels.forEach(aClass -> res.put(getSpecialModelClassForArtefact(aClass), aClass));
        return res;
    }

    public static Class<? extends AbstractYamlArtefact<?>> getSpecialModelClassForArtefact(Class<? extends AbstractArtefact> aClass) {
        return !hasSpecialModelClass(aClass) ? null : aClass.getAnnotation(YamlArtefact.class).model();
    }

    public static List<Class<? extends AbstractYamlArtefact<?>>> getSpecialYamlArtefactModels() {
        return new ArrayList<>(MODEL_TO_ARTEFACT_MAP.keySet());
    }

    private static List<Class<? extends AbstractArtefact>> getArtefactsWithSpecialModels() {
        return AutomationPackageNamedEntityUtils.scanNamedEntityClasses(AbstractArtefact.class).stream()
                .filter(c -> c.isAnnotationPresent(YamlArtefact.class))
                .filter(YamlArtefactsLookuper::hasSpecialModelClass)
                .map(c -> (Class<? extends AbstractArtefact>) c)
                .collect(Collectors.toList());
    }

    public static List<Class<? extends AbstractArtefact>> getSimpleYamlArtefactModels() {
        return AutomationPackageNamedEntityUtils.scanNamedEntityClasses(AbstractArtefact.class).stream()
                .filter(c -> c.isAnnotationPresent(YamlArtefact.class))
                .filter(c -> !hasSpecialModelClass(c))
                .map(c -> (Class<? extends AbstractArtefact>) c)
                .collect(Collectors.toList());
    }

    private static boolean hasSpecialModelClass(Class<?> c) {
        return c.isAnnotationPresent(YamlArtefact.class) && c.getAnnotation(YamlArtefact.class).model() != null && c.getAnnotation(YamlArtefact.class).model() != AbstractYamlArtefact.None.class;
    }

    public static boolean isRootArtefact(Class<?> yamlArtefactClass) {
        Class<? extends AbstractArtefact> artefactClass = getArtefactClass(yamlArtefactClass);
        if (artefactClass == null) {
            return false;
        }
        return artefactClass.getAnnotation(Artefact.class) != null && artefactClass.getAnnotation(Artefact.class).validAsRoot();
    }

    public static boolean isControlArtefact(Class<?> yamlArtefactClass) {
        Class<? extends AbstractArtefact> artefactClass = getArtefactClass(yamlArtefactClass);
        if (artefactClass == null) {
            return false;
        }
        return artefactClass.getAnnotation(Artefact.class) != null && artefactClass.getAnnotation(Artefact.class).validAsControl();
    }

    public static Class<? extends AbstractArtefact> getArtefactClass(Class<?> yamlArtefactClass) {
        if(MODEL_TO_ARTEFACT_MAP.get(yamlArtefactClass) != null){
            // speical model
            return MODEL_TO_ARTEFACT_MAP.get(yamlArtefactClass);
        } else if(AbstractArtefact.class.isAssignableFrom(yamlArtefactClass)){
            // simple model
            return (Class<? extends AbstractArtefact>) yamlArtefactClass;
        } else {
            return null;
        }
    }

    public static String getYamlArtefactName(Class<?> yamlArtefactClass) {
        return AutomationPackageNamedEntityUtils.getEntityNameByClass(yamlArtefactClass);
    }

    public static Class<?> getArtefactModelClassByYamlName(String yamlName) {
        List<Class<? extends AbstractArtefact>> allModels = new ArrayList<>();
        allModels.addAll(getArtefactsWithSpecialModels());
        allModels.addAll(getSimpleYamlArtefactModels());
        for (Class<? extends AbstractArtefact> annotatedClass : allModels) {
            String expectedYamlName = AutomationPackageNamedEntityUtils.getEntityNameByClass(annotatedClass);

            if (yamlName.equalsIgnoreCase(expectedYamlName)) {
                return hasSpecialModelClass(annotatedClass) ? getSpecialModelClassForArtefact(annotatedClass) : annotatedClass;
            }
        }
        return null;
    }

}
