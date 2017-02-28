package step.grid.filemanager;

import step.grid.io.Attachment;

public interface FileProvider {

	public Attachment getFileAsAttachment(String fileId);
}
