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
package step.resources;

import step.core.objectenricher.ObjectEnricher;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LayeredResourceManager implements ResourceManager {

    private final List<ResourceManager> resourceManagers = new ArrayList<>();

    public LayeredResourceManager() {
    }

    public LayeredResourceManager(ResourceManager resourceManager) {
        this.resourceManagers.add(resourceManager);
    }

    public void addManager(ResourceManager manager) {
        resourceManagers.add(manager);
    }

    public void pushManager(ResourceManager manager) {
        resourceManagers.add(0, manager);
    }

    @Override
    public Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException {
        return getManagerForPersistence().createResource(resourceType, resourceStream, resourceFileName, checkForDuplicates, objectEnricher);
    }

    @Override
    public Resource createResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException {
        return getManagerForPersistence().createResource(resourceType, isDirectory, resourceStream, resourceFileName, checkForDuplicates, objectEnricher);
    }

    @Override
    public ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName) throws IOException {
        return getManagerForPersistence().createResourceContainer(resourceType, resourceFileName);
    }

    @Override
    public boolean resourceExists(String resourceId) {
        for (ResourceManager resourceManager : resourceManagers) {
            if(resourceManager.resourceExists(resourceId)){
                return true;
            }
        }
        return false;
    }

    @Override
    public ResourceRevisionContent getResourceContent(String resourceId) throws IOException {
        for (ResourceManager resourceManager : resourceManagers) {
            ResourceRevisionContent found = resourceManager.getResourceContent(resourceId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public ResourceRevisionFileHandle getResourceFile(String resourceId) {
        return layeredLookup(resourceManager -> resourceManager.getResourceFile(resourceId));
    }

    @Override
    public Resource getResource(String resourceId) {
        return layeredLookup(resourceManager -> resourceManager.getResource(resourceId));
    }

    @Override
    public ResourceRevisionContentImpl getResourceRevisionContent(String resourceRevisionId) throws IOException {
        for (ResourceManager resourceManager : resourceManagers) {
            ResourceRevisionContentImpl found = resourceManager.getResourceRevisionContent(resourceRevisionId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public ResourceRevision getResourceRevision(String resourceRevisionId) {
        return layeredLookup(resourceManager -> resourceManager.getResourceRevision(resourceRevisionId));
    }

    @Override
    public String getResourcesRootPath() {
        return getManagerForPersistence().getResourcesRootPath();
    }

    @Override
    public Resource createResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates, ObjectEnricher objectEnricher, String trackingAttribute) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException {
        return getManagerForPersistence().createResource(resourceType, isDirectory, resourceStream, resourceFileName, checkForDuplicates, objectEnricher);
    }

    @Override
    public Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName) throws IOException, InvalidResourceFormatException {
        return getManagerForPersistence().saveResourceContent(resourceId, resourceStream, resourceFileName);
    }

    @Override
    public Resource saveResource(Resource resource) throws IOException {
        return getManagerForPersistence().saveResource(resource);
    }

    @Override
    public ResourceRevision saveResourceRevision(ResourceRevision resourceRevision) throws IOException {
        return getManagerForPersistence().saveResourceRevision(resourceRevision);
    }

    @Override
    public void deleteResource(String resourceId) {
        for (ResourceManager resourceManager : resourceManagers) {
            resourceManager.deleteResource(resourceId);
        }
    }

    @Override
    public List<Resource> findManyByCriteria(Map<String, String> criteria) {
        return layeredSearch(resourceManager -> resourceManager.findManyByCriteria(criteria));
    }

    protected ResourceManager getManagerForPersistence() {
        return resourceManagers.get(0);
    }

    protected <V> V layeredLookup(Function<ResourceManager, V> f) {
        for (ResourceManager rm : resourceManagers) {
            V result = f.apply(rm);
            if(result != null) {
                return result;
            }
        }
        return null;
    }

    protected <V> List<V> layeredSearch(Function<ResourceManager, List<V>> searchFunction) {
        return resourceManagers.stream().map(searchFunction).flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public void cleanup() {
        for (ResourceManager resourceManager : resourceManagers) {
            resourceManager.cleanup();
        }
    }

    @Override
    public ResourceAccessor getResourceAccessor() {
        return getManagerForPersistence().getResourceAccessor();
    }

    public List<ResourceManager> getResourceManagers() {
        return resourceManagers;
    }
}
