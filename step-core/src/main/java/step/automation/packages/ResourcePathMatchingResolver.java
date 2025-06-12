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

import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class ResourcePathMatchingResolver {

    private static final Logger logger = LoggerFactory.getLogger(ResourcePathMatchingResolver.class);
    private final ClassLoader classLoader;

    public ResourcePathMatchingResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public List<URL> getResourcesByPattern(String resourcePathPattern) {
        List<URL> res = new ArrayList<>();
        if(!containsWildcard(resourcePathPattern)){
            res.add(classLoader.getResource(resourcePathPattern));
        } else {
            for (URL resource : findPathMatchingResources(resourcePathPattern)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Obtain resource from automation package: {}", resource);
                }
                res.add(resource);
            }
        }

        return res;
    }

    public static boolean containsWildcard(String resourcePathPattern) {
        return resourcePathPattern.contains("*");
    }

    protected List<URL> findPathMatchingResources(String locationPattern) {
        String[] pathArray = locationPattern.split(getPathSeparator());
        List<URL> result = new ArrayList<>();
        String rootPath = pathArray[0];
        if(containsWildcard(rootPath)) {
            throw new RuntimeException("Wildcards are currently not supported for the root element of the path: " + rootPath + ". You should put all the fragments into a folder and reference them as follow: myFolder/*");
        } else {
            URL resource = classLoader.getResource(rootPath);
            findPathMatchingResourcesRecursive(pathArray, 0, resource, result);
        }
        return result;
    }

    public static String getPathSeparator() {
        return "/";
    }

    private void findPathMatchingResourcesRecursive(String[] pathArray, int currentLevel, URL currentPath, List<URL> result) {
        try {
            if (ClassLoaderResourceFilesystem.isDirectory(currentPath)) {
                int nextLevel = currentLevel + 1;
                if (nextLevel < pathArray.length) {
                    String nextPath = pathArray[nextLevel];
                    List<URL> urls = ClassLoaderResourceFilesystem.listDirectory(currentPath);
                    Pattern pattern = Pattern.compile(nextPath.replaceAll("\\*", ".*"));
                    for (URL url : urls) {
                        String file = url.getFile();
                        if(file.endsWith(getPathSeparator())) {
                            file = file.substring(0, file.length() - 1);
                        }
                        int lastIndexOf = file.lastIndexOf(getPathSeparator());
                        String lastPath = file.substring(lastIndexOf + 1);
                        if (pattern.matcher(lastPath).matches()) {
                            findPathMatchingResourcesRecursive(pathArray, nextLevel, url, result);
                        }
                    }
                }
            } else {
                result.add(currentPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while listing resources", e);
        }
    }

}
