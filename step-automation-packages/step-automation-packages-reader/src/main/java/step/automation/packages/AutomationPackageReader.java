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

import step.automation.packages.model.AutomationPackage;
import step.automation.packages.yaml.AutomationPackageDescriptorReader;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

public class AutomationPackageReader {

    private final AutomationPackageDescriptorReader descriptorReader;

    public AutomationPackageReader(String jsonSchema) {
        this.descriptorReader = new AutomationPackageDescriptorReader(jsonSchema);
    }

    public AutomationPackage readAutomationPackage(AutomationPackageArchive automationPackageArchive) throws AutomationPackageReadingException {
        try {
            if (!automationPackageArchive.isAutomationPackage()) {
                return null;
            }

            try (InputStream yamlInputStream = automationPackageArchive.getDescriptorYaml()) {
                AutomationPackageDescriptorYaml descriptorYaml = descriptorReader.readAutomationPackageDescriptor(yamlInputStream);
                return buildAutomationPackage(descriptorYaml, automationPackageArchive);
            }
        } catch (IOException ex) {
            throw new AutomationPackageReadingException("Unable to read the automation package", ex);
        }
    }

    protected AutomationPackage buildAutomationPackage(AutomationPackageDescriptorYaml descriptor, AutomationPackageArchive archive) throws AutomationPackageReadingException {
        AutomationPackage res = new AutomationPackage();
        res.setName(descriptor.getName());
        // apply imported fragments recursively
        fillAutomationPackageWithImportedFragments(res, descriptor, archive);
        return res;
    }

    public void fillAutomationPackageWithImportedFragments(AutomationPackage targetPackage, AutomationPackageFragmentYaml fragment, AutomationPackageArchive archive) throws AutomationPackageReadingException {
        targetPackage.getKeywords().addAll(fragment.getKeywords());
        targetPackage.getPlans().addAll(fragment.getPlans().stream().map(p -> descriptorReader.getPlanReader().yamlPlanToPlan(p)).collect(Collectors.toList()));
        // TODO: apply scheduler tasks

        if (!fragment.getFragments().isEmpty()) {
            for (String importedFragmentReference : fragment.getFragments()) {
                try (InputStream fragmentYamlStream = archive.getResourceAsStream(importedFragmentReference)) {
                    fragment = descriptorReader.readAutomationPackageFragment(fragmentYamlStream, importedFragmentReference);
                    fillAutomationPackageWithImportedFragments(targetPackage, fragment, archive);
                } catch (IOException e) {
                    throw new AutomationPackageReadingException("Unable to read fragment in automation package: " + importedFragmentReference, e);
                }
            }
        }
    }

    public AutomationPackage readAutomationPackageFromJarFile(File automationPackageJar) throws AutomationPackageReadingException {
        return readAutomationPackage(new AutomationPackageArchive(automationPackageJar));
    }
}
