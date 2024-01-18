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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LayeredResourceManager implements ResourceManager {

    private final List<ResourceManager> resourceManagers = new ArrayList<>();

    private ResourceManager permanentResourceManager = null;

    public LayeredResourceManager() {
    }

    public LayeredResourceManager(ResourceManager resourceManager) {
        addManager(resourceManager, true);
    }

    public LayeredResourceManager(ResourceManager resourceManager, boolean isPermanent) {
        addManager(resourceManager, isPermanent);
    }

    /**
     * Adds the resource manager to the bottom layer
     *
     * @param manager     the resource manager to be added
     * @param isPermanent if true, this resource manager is designed to keep all resources permanently
     *                    (unlike the non-permanent resource managers which resources are temporarily and can be
     *                    automatically deleted)
     */
    public void addManager(ResourceManager manager, boolean isPermanent) {
        resourceManagers.add(manager);
        if (isPermanent) {
            permanentResourceManager = manager;
        }
    }

    /**
     * Adds the resource manager to the top layer
     *
     * @param manager     the resource manager to be added
     * @param isPermanent if true, this resource manager is designed to keep all resources permanently
     *                    (unlike the non-permanent resource managers which resources are temporarily and can be
     *                    automatically deleted)
     */
    public void pushManager(ResourceManager manager, boolean isPermanent) {
        resourceManagers.add(0, manager);
        if (isPermanent) {
            permanentResourceManager = manager;
        }
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
        V result = null;
        ResourceMissingException caught = null;
        for (ResourceManager rm : resourceManagers) {
            try {
                result = f.apply(rm);
            } catch (ResourceMissingException ex) {
                // just ignore the exception
                caught = ex;
            }
            if (result != null) {
                return result;
            }
        }

        // if nothing is found we have to re-throw the exception from underlying resource manager
        if (caught != null) {
            throw caught;
        } else {
            return null;
        }
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

    public List<ResourceManager> getResourceManagers() {
        return Collections.unmodifiableList(resourceManagers);
    }

    /**
     * Returns the nested resource manager marked as permanent. I.e. all resources stored in this resource manager
     * will be stored permanently and won't be automatically deleted as temporary resources.
     */
    public ResourceManager getPermanentResourceManager() {
        if (permanentResourceManager != null) {
            if (permanentResourceManager instanceof LayeredResourceManager) {
                return ((LayeredResourceManager) permanentResourceManager).getPermanentResourceManager();
            } else {
                return permanentResourceManager;
            }
        } else {
            return null;
        }
    }
}
