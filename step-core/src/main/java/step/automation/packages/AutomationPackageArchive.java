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

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Objects;


public abstract class AutomationPackageArchive implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageArchive.class);
    public static final List<String> METADATA_FILES = List.of("automation-package.yml", "automation-package.yaml");
    public static final String NULL_TYPE_ERROR_MSG = "The type of the AutomationPackageArchive must not be null";

    private final File originalFile;
    private final File keywordLibFile;
    private final String type;

    protected AutomationPackageArchive(String type) {
        this.originalFile = null;
        this.keywordLibFile = null;
        Objects.requireNonNull(type, NULL_TYPE_ERROR_MSG);
        this.type = type;
    }

    public AutomationPackageArchive(File automationPackageFile, File keywordLibFile, String type) throws AutomationPackageReadingException {
        Objects.requireNonNull(automationPackageFile, "The automationPackageFile must not be null");
        Objects.requireNonNull(automationPackageFile, NULL_TYPE_ERROR_MSG);
        if (!automationPackageFile.exists()) {
            throw new AutomationPackageReadingException("Automation package " + automationPackageFile.getName() + " doesn't exist");
        }
        this.originalFile = automationPackageFile;
        this.keywordLibFile = keywordLibFile;
        this.type = type;
    }

    abstract public boolean hasAutomationPackageDescriptor();

    abstract public InputStream getDescriptorYaml();

    abstract public InputStream getResourceAsStream(String resourcePath) throws IOException;

    abstract public URL getResource(String resourcePath) ;

    abstract public List<URL> getResourcesByPattern(String resourcePathPattern);

    public File getKeywordLibFile(){
        return keywordLibFile;
    }

    public File getOriginalFile() {
        return originalFile;
    }

    public String getOriginalFileName() {
        return originalFile == null ? null : originalFile.getName();
    }

    public String getType() {
        return type;
    }

    abstract public ResourcePathMatchingResolver getResourcePathMatchingResolver();
}
