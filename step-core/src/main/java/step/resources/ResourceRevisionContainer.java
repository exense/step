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

import java.io.IOException;
import java.io.OutputStream;

import step.core.objectenricher.ObjectEnricher;

public class ResourceRevisionContainer {

	protected final Resource resource;
	protected final ResourceRevision resourceRevision;
	protected final OutputStream outputStream;
	private final ResourceManagerImpl resourceManagerImpl;

	protected ResourceRevisionContainer(Resource resource, ResourceRevision resourceRevision, OutputStream outputStream, ResourceManagerImpl resourceManagerImpl) {
		super();
		this.resource = resource;
		this.resourceRevision = resourceRevision;
		this.outputStream = outputStream;
		this.resourceManagerImpl = resourceManagerImpl;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}
	
	public Resource getResource() {
		return resource;
	}

	public ResourceRevision getResourceRevision() {
		return resourceRevision;
	}

	public void save(boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException {
		try {
			outputStream.close();
		} catch (IOException e) {

		}
		resourceManagerImpl.closeResourceContainer(resource, resourceRevision, checkForDuplicates, objectEnricher);
	}
	
	public void save(ObjectEnricher objectEnricher) throws IOException {
		try {
			save(false, objectEnricher);
		} catch (SimilarResourceExistingException e) {
			throw new RuntimeException("This should never happen");
		}
	}

	public void save() throws IOException {
		save(null);
	}
}
