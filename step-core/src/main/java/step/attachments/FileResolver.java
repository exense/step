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

import org.bson.types.ObjectId;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionFileHandle;

public class FileResolver {

	public static final String ATTACHMENT_PREFIX = "attachment:";
	public static final String RESOURCE_PREFIX = "resource:";
	public static final String RESOURCE_PATH_SEPARATOR = ":";
	
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
			file = getResourceRevisionFileHandleForPath(path).getResourceFile();
		} else {
			file = new File(path);
		}
		return file;
	}
	
	public static String resolveResourceId(String path) {
		String resourceId;
		if(path != null && path.startsWith(RESOURCE_PREFIX)) {
			String subResourcePath = extractResourceSubPath(path);
			resourceId = subResourcePath.split(RESOURCE_PATH_SEPARATOR)[0];
		} else {
			resourceId = null;
		}
		return resourceId;
	}

	public static String resolveRevisionId(String path) {
		String revisionId = null;
		if(path != null && path.startsWith(RESOURCE_PREFIX)) {
			String subResourcePath = extractResourceSubPath(path);
			String[] split = subResourcePath.split(RESOURCE_PATH_SEPARATOR);
			if (split.length == 2){
				revisionId = split[1];
			}
		}
		if (revisionId == null || !ObjectId.isValid(revisionId)) {
			throw new RuntimeException("Invalid revision path: "  + path);
		}
		return revisionId;
	}
	
	public static boolean isResource(String path) {
		return path != null && path.startsWith(RESOURCE_PREFIX);
	}

	public static String createPathForResource(Resource resource) {
		return createPathForResourceId(resource.getId().toString());
	}
	
	public static String createPathForResourceId(String resourceId) {
		return RESOURCE_PREFIX + resourceId;
	}

	public static String createRevisionPathForResource(Resource resource) {
		return RESOURCE_PREFIX + resource.getId().toHexString() + RESOURCE_PATH_SEPARATOR + resource.getCurrentRevisionId().toHexString();
	}

	protected static String extractResourceSubPath(String path) {
		return path.replace(RESOURCE_PREFIX, "");
	}
	
	public FileHandle resolveFileHandle(String path) {
		File file;
		ResourceRevisionFileHandle resourceRevisionFileHandle;
		if(path.startsWith(ATTACHMENT_PREFIX)) {
			throw new RuntimeException("Attachments have been migrated to the ResourceManager. The reference " + path +
					" isn't valid anymore. Your attachment should be migrated to the ResourceManager.");
		} else if(path.startsWith(RESOURCE_PREFIX)) {
			resourceRevisionFileHandle = getResourceRevisionFileHandleForPath(path);
			file = resourceRevisionFileHandle.getResourceFile();
		} else {
			file = new File(path);
			resourceRevisionFileHandle = null;
		}
		return new FileHandle(file, resourceRevisionFileHandle);
	}

	private ResourceRevisionFileHandle getResourceRevisionFileHandleForPath(String path) {
		ResourceRevisionFileHandle resourceRevisionFileHandle;
		String subResourcePath = extractResourceSubPath(path);
		String[] split = subResourcePath.split(RESOURCE_PATH_SEPARATOR);
		if (split.length == 1) {
			resourceRevisionFileHandle = resourceManager.getResourceFile(split[0]);
		} else if (split.length == 2) {
			resourceRevisionFileHandle = resourceManager.getResourceFile(split[0], split[1]);
		} else {
			throw new RuntimeException("Invalid resource path: "  + path);
		}
		return resourceRevisionFileHandle;
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
