package step.grid.filemanager;

import java.io.File;

public interface FileManagerClient {

	File requestFile(String uid, long lastModified);
	
	FileVersion requestFileVersion(String uid, long lastModified);
	
	static class FileVersion {
		
		File file;
		
		boolean modified;
		
		String fileId;

		public String getFileId() {
			return fileId;
		}

		public void setFileId(String fileId) {
			this.fileId = fileId;
		}

		public File getFile() {
			return file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		public boolean isModified() {
			return modified;
		}

		public void setModified(boolean modified) {
			this.modified = modified;
		}
	}

}