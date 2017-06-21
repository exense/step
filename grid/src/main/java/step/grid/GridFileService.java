package step.grid;

import java.io.File;

public interface GridFileService {

	String registerFile(File file);
	
	File getRegisteredFile(String fileHandle);

}