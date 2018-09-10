package step.grid.filemanager;

import java.io.File;
import java.io.IOException;

public interface StreamingFileProvider {
	
	public File saveFileTo(String fileHandle, File file) throws IOException;
	
}
