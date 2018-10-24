package step.grid;

import java.io.File;

public interface GridFileService {

	/**
	 * Register a file into the GRID
	 * 
	 * @param file the file to be registered to the GRID
	 * @return an handle to the registered file. This handle will be used to retrieve the registered file
	 */
	String registerFile(File file);
	
	/**
	 * Get a file that has been previously registered to the GRID
	 * 
	 * @param fileHandle the handle returned at regitration
	 * @return the registered file
	 */
	File getRegisteredFile(String fileHandle);

}