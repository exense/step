package step.attachments;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import step.resources.ResourceManager;
import step.resources.ResourceRevisionFileHandle;

public class FileResolver {

	public static final String ATTACHMENT_PREFIX = "attachment:";
	public static final String RESOURCE_PREFIX = "resource:";
	
	private ResourceManager resourceManager;
	
	
	public FileResolver(ResourceManager resourceManager) {
		super();
		this.resourceManager = resourceManager;
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
