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
import java.util.List;
import java.util.stream.Collectors;

public class YamlArtefactsLookuper {

    public static List<Class<? extends AbstractYamlArtefact<?>>> getSpecialYamlArtefactModels() {
        return AutomationPackageNamedEntityUtils.scanNamedEntityClasses(AbstractYamlArtefact.class).stream()
                .filter(c -> c.isAnnotationPresent(YamlArtefact.class))
                .map(c -> (Class<? extends AbstractYamlArtefact<?>>) c)
                .collect(Collectors.toList());
    }

    public static List<Class<? extends AbstractArtefact>> getSimpleYamlArtefactModels() {
        return AutomationPackageNamedEntityUtils.scanNamedEntityClasses(AbstractArtefact.class).stream()
                .filter(c -> c.isAnnotationPresent(YamlArtefact.class))
                .map(c -> (Class<? extends AbstractArtefact>) c)
                .collect(Collectors.toList());
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
        YamlArtefact ann = yamlArtefactClass.getAnnotation(YamlArtefact.class);
        if (ann == null) {
            return null;
        }

        Class<? extends AbstractArtefact> artefactClass;
        if (ann.forClass() != null && !ann.forClass().equals(AbstractArtefact.None.class)) {
            artefactClass = ann.forClass();
        } else {
            artefactClass = (Class<? extends AbstractArtefact>) yamlArtefactClass;
        }
        return artefactClass;
    }

    public static String getYamlArtefactName(Class<?> yamlArtefactClass) {
        return AutomationPackageNamedEntityUtils.getEntityNameByClass(yamlArtefactClass);
    }

    public static Class<?> getArtefactClassByYamlName(String yamlName) {
        List<Class<?>> allModels = new ArrayList<>();
        allModels.addAll(getSpecialYamlArtefactModels());
        allModels.addAll(getSimpleYamlArtefactModels());
        for (Class<?> annotatedClass : allModels) {
            String expectedYamlName = AutomationPackageNamedEntityUtils.getEntityNameByClass(annotatedClass);

            if (yamlName.equalsIgnoreCase(expectedYamlName)) {
                return annotatedClass;
            }
        }
        return null;
    }

}
