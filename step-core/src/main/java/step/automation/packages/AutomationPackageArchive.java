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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.util.List;

import static step.automation.packages.AutomationPackageArchiveType.JAVA;

public class AutomationPackageArchive implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageArchive.class);
    public static final List<String> METADATA_FILES = List.of("automation-package.yml", "automation-package.yaml");

    private final ClassLoader classLoader;
    private final File originalFile;
    private boolean internalClassLoader = false;
    private final AutomationPackageArchiveType type;
    private final ResourcePathMatchingResolver pathMatchingResourceResolver;

    public AutomationPackageArchive(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.originalFile = null;
        this.pathMatchingResourceResolver = new ResourcePathMatchingResolver(classLoader);
        this.type = JAVA;
    }

    public AutomationPackageArchive(File automationPackageFile) throws AutomationPackageReadingException {
        this.internalClassLoader = true;
        if (!automationPackageFile.exists()) {
            throw new AutomationPackageReadingException("Automation package " + automationPackageFile.getName() + " doesn't exist");
        }
        if (!automationPackageFile.isDirectory() && !isArchive(automationPackageFile)) {
            throw new AutomationPackageReadingException("Automation package " + automationPackageFile.getName() + " is neither zip archive nor jar file nor directory");
        }
        this.originalFile = automationPackageFile;
        this.type = JAVA; //Only supported type for now
        try {
            this.classLoader = new URLClassLoader(new URL[]{automationPackageFile.toURI().toURL()}, null);
            this.pathMatchingResourceResolver = new ResourcePathMatchingResolver(classLoader);
        } catch (MalformedURLException ex) {
            throw new AutomationPackageReadingException("Unable to read automation package", ex);
        }
    }

    private static boolean isArchive(File f) {
        int fileSignature = 0;
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            fileSignature = raf.readInt();
        } catch (IOException e) {
            // handle if you like
        }
        return fileSignature == 0x504B0304 || fileSignature == 0x504B0506 || fileSignature == 0x504B0708;
    }

    public boolean hasAutomationPackageDescriptor() {
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

    public InputStream getResourceAsStream(String resourcePath) throws IOException {
        URL url = getResource(resourcePath);
        return url.openStream();
    }

    public URL getResource(String resourcePath) {
        URL resource = classLoader.getResource(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug("Obtain resource from automation package: {}", resource);
        }
        return resource;
    }

    public List<URL> getResourcesByPattern(String resourcePathPattern) {
        return pathMatchingResourceResolver.getResourcesByPattern(resourcePathPattern);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public File getOriginalFile() {
        return originalFile;
    }

    public String getOriginalFileName() {
        return originalFile == null ? null : originalFile.getName();
    }

    @Override
    public void close() throws IOException {
        if (internalClassLoader && this.classLoader instanceof Closeable) {
            ((Closeable) this.classLoader).close();
        }
    }

    public AutomationPackageArchiveType getType() {
        return type;
    }
}
