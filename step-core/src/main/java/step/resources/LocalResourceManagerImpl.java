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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class LocalResourceManagerImpl extends ResourceManagerImpl {

	public LocalResourceManagerImpl() {
		super(new File("resources"), new InMemoryResourceAccessor(), new InMemoryResourceRevisionAccessor());
	}
	
	public LocalResourceManagerImpl(File folder) {
		super(folder, new InMemoryResourceAccessor(), new InMemoryResourceRevisionAccessor());
	}
	
	public LocalResourceManagerImpl(File resourceRootFolder, ResourceAccessor resourceAccessor,
			ResourceRevisionAccessor resourceRevisionAccessor) {
		super(resourceRootFolder, resourceAccessor, resourceRevisionAccessor);
	}

	public ResourceRevisionAccessor getResourceRevisionAccessor() {
		return resourceRevisionAccessor;
	}

	@Override
	public void cleanup() {
		if (resourceRootFolder.exists() && resourceRootFolder.isDirectory() && resourceRootFolder.canWrite()) {
			try {
				FileUtils.deleteDirectory(resourceRootFolder);
			} catch (IOException e) {
				logger.error("Could not remove local resource folder: " + resourceRootFolder.getAbsolutePath(),e );
			}
		}
	}

}
