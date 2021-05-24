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
import java.io.IOException;
import java.io.InputStream;


public class ResourceRevisionContentImpl implements Closeable, ResourceRevisionContent {

	private final InputStream resourceStream;
	private final String resourceName;
	private final Resource resource;
	private final ResourceManagerImpl resourceManager;
	
	protected ResourceRevisionContentImpl (ResourceManagerImpl resourceManager, Resource resource, InputStream resourceStream, String resourceName) {
		super();
		this.resource = resource;
		this.resourceStream = resourceStream;
		this.resourceName = resourceName;
		this.resourceManager = resourceManager;
	}

	@Override
	public InputStream getResourceStream() {
		return resourceStream;
	}

	@Override
	public String getResourceName() {
		return resourceName;
	}

	@Override
	public void close() throws IOException {
		resourceStream.close();
		resourceManager.closeResourceRevisionContent(resource);
	}
}
