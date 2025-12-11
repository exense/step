/*
 * Copyright (C) 2025, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.functions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.grid.contextbuilder.ApplicationContextFactory;
import step.grid.contextbuilder.ClassPathHelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class FileApplicationContextFactory extends ApplicationContextFactory {

    private static final Logger logger = LoggerFactory.getLogger(FileApplicationContextFactory.class);

    private final File jar;
    private URLClassLoader urlClassLoader;

    public FileApplicationContextFactory(File jar) {
        this.jar = jar;
    }

    public String getId() {
        return this.jar.getAbsolutePath();
    }

    public boolean requiresReload() {
        return false;
    }

    public ClassLoader buildClassLoader(ClassLoader parentClassLoader) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating URLClassLoader from extracted local file {}", this.jar.getAbsolutePath());
        }
        List<URL> urls = ClassPathHelper.forSingleFile(this.jar);
        URL[] urlArray = urls.toArray(new URL[urls.size()]);
        urlClassLoader = new URLClassLoader(urlArray, parentClassLoader);
        return urlClassLoader;
    }

    public void onClassLoaderClosed() {
        if (urlClassLoader != null) {
            try {
                urlClassLoader.close();
            } catch (IOException e) {
                logger.error("Unable to close the classloader for {}", jar.getAbsolutePath(), e);
            }
        }
    }
}
