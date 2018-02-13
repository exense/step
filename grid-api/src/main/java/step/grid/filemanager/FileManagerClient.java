package step.grid.filemanager;

import java.io.File;
import java.io.IOException;

public interface FileManagerClient {

	File requestFile(String uid, long lastModified);
	
	FileVersion requestFileVersion(String uid, long lastModified) throws IOException;
	
	String getDataFolderPath();
	
	public static class FileVersionId {
		
		String fileId;
		
		long version;

		public FileVersionId() {
			super();
		}

		public FileVersionId(String fileId, long version) {
			super();
			this.fileId = fileId;
			this.version = version;
		}

		public String getFileId() {
			return fileId;
		}

		public void setFileId(String fileId) {
			this.fileId = fileId;
		}

		public long getVersion() {
			return version;
		}

		public void setVersion(long version) {
			this.version = version;
		}
	}
	
	static class FileVersion {
		
		File file;
		
		boolean modified;
		
		String fileId;
		
		long version;

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

		public long getVersion() {
			return version;
		}

		public void setVersion(long version) {
			this.version = version;
		}
	}

}