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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.scanner.AnnotationScanner;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.util.ArrayList;
import java.util.List;

import static step.automation.packages.AutomationPackageArchiveType.JAVA;

public class AutomationPackageArchive implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageArchive.class);
    public static final List<String> METADATA_FILES = List.of("automation-package.yml", "automation-package.yaml");

    private final ClassLoader classLoaderForMainApFile;
    private final ClassLoader classLoaderForApAndLibraries;
    private final File originalFile;
    private final File keywordLibFile;
    private boolean internalClassLoader = false;
    private final AutomationPackageArchiveType type;
    private final ResourcePathMatchingResolver pathMatchingResourceResolver;

    public AutomationPackageArchive(ClassLoader classLoader) {
        this.classLoaderForMainApFile = classLoader;
        this.classLoaderForApAndLibraries = classLoader;
        this.originalFile = null;
        this.keywordLibFile = null;
        this.pathMatchingResourceResolver = new ResourcePathMatchingResolver(classLoader);
        this.type = JAVA;
    }

    public AutomationPackageArchive(File automationPackageFile, File keywordLibFile) throws AutomationPackageReadingException {
        this.internalClassLoader = true;
        if (!automationPackageFile.exists()) {
            throw new AutomationPackageReadingException("Automation package " + automationPackageFile.getName() + " doesn't exist");
        }
        if (!automationPackageFile.isDirectory() && !isArchive(automationPackageFile)) {
            throw new AutomationPackageReadingException("Automation package " + automationPackageFile.getName() + " is neither zip archive nor jar file nor directory");
        }
        this.originalFile = automationPackageFile;
        this.keywordLibFile = keywordLibFile;
        this.type = JAVA; //Only supported type for now
        try {
            // IMPORTANT!!! The class loader for descriptor file should contain only the main AP file and should not use the parent classloader
            this.classLoaderForMainApFile = new URLClassLoader(new URL[]{automationPackageFile.toURI().toURL()}, null);
            this.pathMatchingResourceResolver = new ResourcePathMatchingResolver(classLoaderForMainApFile);

            // IMPORTANT!!! The class loader used to scan plans and keywords by annotations should contain all the classes from AP file and keyword lib
            // (inclusive the parent classloader)
            this.classLoaderForApAndLibraries = createClassloaderForApWithKeywordLib(automationPackageFile, keywordLibFile);
        } catch (MalformedURLException ex) {
            throw new AutomationPackageReadingException("Unable to read automation package", ex);
        }
    }

    public static URLClassLoader createClassloaderForApWithKeywordLib(File automationPackageFile, File keywordLibFile) throws MalformedURLException {
        List<URL> classLoaderUrls = new ArrayList<>();
        classLoaderUrls.add(automationPackageFile.toURI().toURL());
        if (keywordLibFile != null) {
            classLoaderUrls.add(keywordLibFile.toURI().toURL());
        }
        return new URLClassLoader(classLoaderUrls.toArray(new URL[]{}));
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
            URL metadataFileUrl = classLoaderForMainApFile.getResource(metadataFile);
            if (metadataFileUrl != null) {
                return true;
            }
        }
        return false;
    }

    public InputStream getDescriptorYaml() {
        for (String metadataFile : METADATA_FILES) {
            InputStream yamlDescriptor = classLoaderForMainApFile.getResourceAsStream(metadataFile);
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
        URL resource = classLoaderForMainApFile.getResource(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug("Obtain resource from automation package: {}", resource);
        }
        return resource;
    }

    public List<URL> getResourcesByPattern(String resourcePathPattern) {
        return pathMatchingResourceResolver.getResourcesByPattern(resourcePathPattern);
    }

    public ClassLoader getClassLoaderForMainApFile() {
        return classLoaderForMainApFile;
    }

    public ClassLoader getClassLoaderForApAndLibraries() {
        return classLoaderForApAndLibraries;
    }

    public AnnotationScanner createAnnotationScanner(){
        // for file-based packages we create class loader for file, otherwise we just use class loader from archive
        if (getOriginalFile() != null) {
            return AnnotationScanner.forSpecificJarFromURLClassLoader((URLClassLoader) getClassLoaderForApAndLibraries());
        } else {
            return AnnotationScanner.forAllClassesFromClassLoader(getClassLoaderForApAndLibraries());
        }
    }

    public File getKeywordLibFile(){
        return keywordLibFile;
    }

    public File getOriginalFile() {
        return originalFile;
    }

    public String getOriginalFileName() {
        return originalFile == null ? null : originalFile.getName();
    }

    @Override
    public void close() throws IOException {
        if (internalClassLoader && this.classLoaderForMainApFile instanceof Closeable) {
            IOUtils.closeQuietly(((Closeable) this.classLoaderForMainApFile));
        }
        if (internalClassLoader && this.classLoaderForApAndLibraries instanceof Closeable) {
            IOUtils.closeQuietly(((Closeable) this.classLoaderForApAndLibraries));
        }
    }

    public AutomationPackageArchiveType getType() {
        return type;
    }
}
