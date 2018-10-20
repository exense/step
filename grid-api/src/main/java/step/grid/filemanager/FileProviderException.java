package step.grid.filemanager;

@SuppressWarnings("serial")
public class FileProviderException extends Exception {

	private final String fileHandle;
	
	public FileProviderException(String fileHandle, Throwable cause) {
		super(cause);
		this.fileHandle = fileHandle;
	}

	public String getFileHandle() {
		return fileHandle;
	}
}
