package step.grid.filemanager;

import java.io.File;

public interface StreamingFileProvider {
	
	public File saveFileTo(String fileHandle, File file) throws FileProviderException;
	
}
