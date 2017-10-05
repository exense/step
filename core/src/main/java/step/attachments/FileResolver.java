package step.attachments;

import java.io.File;

public class FileResolver {

	private static final String ATTACHMENT_PREFIX = "attachment:";
	
	private AttachmentManager attachmentManager;
	
	public FileResolver(AttachmentManager attachmentManager) {
		super();
		this.attachmentManager = attachmentManager;
	}

	public File resolve(String path) {
		File file;
		if(path.startsWith(ATTACHMENT_PREFIX)) {
			file = attachmentManager.getFileById(path.replace(ATTACHMENT_PREFIX, ""));
		} else {
			file = new File(path);
		}
		return file;
	}
}
