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
package step.attachments;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import step.resources.ResourceManager;
import step.resources.ResourceRevisionFileHandle;

public class FileResolver {

	public static final String ATTACHMENT_PREFIX = "attachment:";
	public static final String RESOURCE_PREFIX = "resource:";
	
	private final ResourceManager resourceManager;
	
	public FileResolver(ResourceManager resourceManager) {
		super();
		this.resourceManager = resourceManager;
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public File resolve(String path) {
		File file;
		if(path.startsWith(ATTACHMENT_PREFIX)) {
			throw new RuntimeException("Attachments have been migrated to the ResourceManager. The reference " + path +
					" isn't valid anymore. Your attachment should be migrated to the ResourceManager.");
		} else if(path.startsWith(RESOURCE_PREFIX)) {
			String resourceId = extractResourceId(path);
			file = resourceManager.getResourceFile(resourceId).getResourceFile();
		} else {
			file = new File(path);
		}
		return file;
	}
	
	public String resolveResourceId(String path) {
		String resourceId;
		if(path != null && path.startsWith(RESOURCE_PREFIX)) {
			resourceId = extractResourceId(path);
		} else {
			resourceId = null;
		}
		return resourceId;
	}

	protected String extractResourceId(String path) {
		return path.replace(RESOURCE_PREFIX, "");
	}
	
	public FileHandle resolveFileHandle(String path) {
		File file;
		ResourceRevisionFileHandle resourceRevisionFileHandle;
		if(path.startsWith(ATTACHMENT_PREFIX)) {
			throw new RuntimeException("Attachments have been migrated to the ResourceManager. The reference " + path +
					" isn't valid anymore. Your attachment should be migrated to the ResourceManager.");
		} else if(path.startsWith(RESOURCE_PREFIX)) {
			String resourceId = extractResourceId(path);
			ResourceRevisionFileHandle resourceFile = resourceManager.getResourceFile(resourceId);
			resourceRevisionFileHandle = resourceFile;
			file = resourceFile.getResourceFile();
		} else {
			file = new File(path);
			resourceRevisionFileHandle = null;
		}
		return new FileHandle(file, resourceRevisionFileHandle);
	}
	
	public class FileHandle implements Closeable {
		
		protected final File file;
		protected final ResourceRevisionFileHandle resourceRevisionFileHandle;

		public FileHandle(File file, ResourceRevisionFileHandle resourceRevisionFileHandle) {
			super();
			this.file = file;
			this.resourceRevisionFileHandle = resourceRevisionFileHandle;
		}

		public File getFile() {
			return file;
		}

		@Override
		public void close() throws IOException {
			if(resourceRevisionFileHandle!=null) {
				resourceRevisionFileHandle.close();
			}
		}
	}
}
