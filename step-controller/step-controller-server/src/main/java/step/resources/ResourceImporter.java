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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;

import org.bson.types.ObjectId;

import step.core.imports.ImportConfiguration;
import step.core.imports.ImportContext;


public class ResourceImporter implements BiConsumer<Object, ImportContext> {

	private ResourceManager resourceManager;
	
	public ResourceImporter(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}
	
	@Override
	public void accept(Object t, ImportContext importContext) {
		if(t instanceof Resource) {
			Resource resource = (Resource) t;

			ImportConfiguration importConfiguration = importContext.getImportConfiguration();
			LocalResourceManagerImpl localResourceMgr = importContext.getLocalResourceMgr();
			
			String origResourceId;
			if(importConfiguration.isOverwrite()) {
				origResourceId = resource.getId().toString(); 
			} else {
				origResourceId = importContext.getNewToOldReferences().get(resource.getId().toString());
			}
			
			// This ugly workaround is needed to be able to get resource file from local manager
			saveCopyOfResourceToLocalResourceManager(localResourceMgr, resource, origResourceId);
			
			File resourceFile = localResourceMgr.getResourceFile(origResourceId).getResourceFile();
			try (InputStream fileInputStream = new FileInputStream(resourceFile)){
				Resource newResource = resourceManager.saveResourceContent(resource.getId().toString(), fileInputStream, resource.getResourceName());
				// Update the revision id
				resource.setCurrentRevisionId(newResource.getCurrentRevisionId());
			} catch (IOException e) {
				throw new RuntimeException("Error while updating resource content for resource "+origResourceId, e);
			}
		}
	}

	public void saveCopyOfResourceToLocalResourceManager(LocalResourceManagerImpl localResourceMgr, Resource resource,
			String origResourceId) {
		try {
			Resource resource2 = new Resource();
			resource2.setResourceName(resource.getResourceName());
			resource2.setId(new ObjectId(origResourceId));
			resource2.setCurrentRevisionId(resource.getCurrentRevisionId());
			resource2.setResourceType(resource.getResourceType());
			localResourceMgr.saveResource(resource2);
			
			ObjectId currentRevisionId = resource.getCurrentRevisionId();
			ResourceRevision revision = new ResourceRevision();
			revision.setId(currentRevisionId);
			revision.setResourceId(resource2.getId().toString());
			revision.setResourceFileName(resource2.getResourceName());
			localResourceMgr.saveResourceRevision(revision);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected error while creating temporary resource for resource "+origResourceId, e);
		}
	}
}
