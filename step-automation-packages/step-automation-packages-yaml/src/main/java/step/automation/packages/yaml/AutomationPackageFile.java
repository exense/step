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
package step.automation.packages.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.yaml.model.AutomationPackageReadingException;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class AutomationPackageFile {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageFile.class);
    static final List<String> METADATA_FILES = List.of("automation.yml", "automation.yaml");

    private final URLClassLoader classLoader;

    public AutomationPackageFile(File automationPackageJar) throws AutomationPackageReadingException {
        try {
            this.classLoader = new URLClassLoader(new URL[]{automationPackageJar.toURI().toURL()});
        } catch (MalformedURLException ex){
            throw new AutomationPackageReadingException("Unable to read automation package", ex);
        }
    }

    public boolean isAutomationPackage() {
        for (String metadataFile : METADATA_FILES) {
            URL metadataFileUrl = classLoader.getResource(metadataFile);
            if (metadataFileUrl != null) {
                return true;
            }
        }
        return false;
    }

    public InputStream getDescriptorYaml() {
        for (String metadataFile : METADATA_FILES) {
            InputStream yamlDescriptor = classLoader.getResourceAsStream(metadataFile);
            if (yamlDescriptor != null) {
                return yamlDescriptor;
            }
        }
        return null;
    }

}
