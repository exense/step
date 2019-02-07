package step.attachments;

import java.io.File;

import step.resources.ResourceManager;

public class FileResolver {

	private static final String ATTACHMENT_PREFIX = "attachment:";
	private static final String RESOURCE_PREFIX = "resource:";
	
	private AttachmentManager attachmentManager;
	private ResourceManager resourceManager;
	
	
	public FileResolver(AttachmentManager attachmentManager, ResourceManager resourceManager) {
		super();
		this.attachmentManager = attachmentManager;
		this.resourceManager = resourceManager;
	}

	public File resolve(String path) {
		File file;
		if(path.startsWith(ATTACHMENT_PREFIX)) {
			file = attachmentManager.getFileById(path.replace(ATTACHMENT_PREFIX, ""));
		} else if(path.startsWith(RESOURCE_PREFIX)) {
			String resourceId = path.replace(RESOURCE_PREFIX, "");
			file = resourceManager.getResourceFile(resourceId);
		} else {
			file = new File(path);
		}
		return file;
	}
}
