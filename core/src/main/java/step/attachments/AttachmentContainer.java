package step.attachments;

import java.io.File;

public class AttachmentContainer {

	AttachmentMeta meta;
	
	File container;

	public AttachmentMeta getMeta() {
		return meta;
	}

	public void setMeta(AttachmentMeta meta) {
		this.meta = meta;
	}

	public File getContainer() {
		return container;
	}

	public void setContainer(File container) {
		this.container = container;
	}
	
}
