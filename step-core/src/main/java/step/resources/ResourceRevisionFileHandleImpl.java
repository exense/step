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

import ch.exense.commons.core.model.resources.Resource;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class ResourceRevisionFileHandleImpl implements Closeable, ResourceRevisionFileHandle {

	private final ResourceManagerImpl resourceManager;
	private final Resource resource;
	private final File resourceFile;
	
	public ResourceRevisionFileHandleImpl(ResourceManagerImpl resourceManager, Resource resource, File resourceFile) {
		super();
		this.resourceManager = resourceManager;
		this.resource = resource;
		this.resourceFile = resourceFile;
	}

	@Override
	public File getResourceFile() {
		return resourceFile;
	}

	@Override
	public void close() throws IOException {
		resourceManager.closeResourceRevisionContent(resource);
	}
}
