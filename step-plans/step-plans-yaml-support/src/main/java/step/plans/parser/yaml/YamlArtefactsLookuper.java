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

import java.util.List;
import java.util.stream.Collectors;

public class YamlArtefactsLookuper {

    public static List<Class<? extends AbstractYamlArtefact<?>>> getYamlArtefactClasses() {
        return AutomationPackageNamedEntityUtils.scanNamedEntityClasses(AbstractYamlArtefact.class).stream()
                .map(c -> (Class<? extends AbstractYamlArtefact<?>>) c)
                .collect(Collectors.toList());
    }

    public static boolean isRootArtefact(Class<? extends AbstractYamlArtefact<?>> yamlArtefactClass) {
        Artefact ann = createYamlArtefactInstance(yamlArtefactClass).getArtefactClass().getAnnotation(Artefact.class);
        if (ann == null) {
            return false;
        }
        return ann.validAsRoot();
    }

    public static boolean isControlArtefact(Class<? extends AbstractYamlArtefact<?>> yamlArtefactClass) {
        Artefact ann = createYamlArtefactInstance(yamlArtefactClass).getArtefactClass().getAnnotation(Artefact.class);
        if (ann == null) {
            return false;
        }
        return ann.validAsControl();
    }

    public static Class<? extends AbstractArtefact> getArtefactClass(Class<? extends AbstractYamlArtefact<?>> yamlArtefactClass) {
        return createYamlArtefactInstance(yamlArtefactClass).getArtefactClass();
    }

    public static String getYamlArtefactName(Class<? extends AbstractYamlArtefact<?>> yamlArtefactClass) {
        return AbstractArtefact.getArtefactName(getArtefactClass(yamlArtefactClass));
    }

    private static AbstractYamlArtefact<?> createYamlArtefactInstance(Class<? extends AbstractYamlArtefact<?>> yamlArtefactClass) {
        try {
            return yamlArtefactClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Yaml artefact exception", e);
        }
    }
}
