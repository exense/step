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
import java.util.List;
import java.util.stream.Collectors;

public class AutomationPackageReader {

    private final AutomationPackageDescriptorReader descriptorReader;

    public AutomationPackageReader() {
        this.descriptorReader = new AutomationPackageDescriptorReader();
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
        // TODO: apply scheduler tasks
        res.setPlans(descriptor.getPlans().stream().map(p -> descriptorReader.getPlanReader().yamlPlanToPlan(p)).collect(Collectors.toList()));
        res.setKeywords(descriptor.getKeywords());
        importIntoAutomationPackage(res, descriptor.getFragments(), archive);
        return res;
    }

    public void importIntoAutomationPackage(AutomationPackage targetPackage, List<String> imports, AutomationPackageArchive archive) throws AutomationPackageReadingException {
        for (String fragmentReference : imports) {
            AutomationPackageFragmentYaml fragment;
            try (InputStream fragmentYamlStream = archive.getResourceAsStream(fragmentReference)) {
                fragment = descriptorReader.readAutomationPackageFragment(fragmentYamlStream, fragmentReference);
                targetPackage.getKeywords().addAll(fragment.getKeywords());
                targetPackage.getPlans().addAll(fragment.getPlans().stream().map(p -> descriptorReader.getPlanReader().yamlPlanToPlan(p)).collect(Collectors.toList()));
                // TODO: apply scheduler tasks
            } catch (IOException e) {
                throw new AutomationPackageReadingException("Unable to read fragment in automation package: " + fragmentReference, e);
            }

            if (!fragment.getFragments().isEmpty()) {
                importIntoAutomationPackage(targetPackage, fragment.getFragments(), archive);
            }
        }
    }

    public AutomationPackage readAutomationPackageFromJarFile(File automationPackageJar) throws AutomationPackageReadingException {
        return readAutomationPackage(new AutomationPackageArchive(automationPackageJar));
    }
}
