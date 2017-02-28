package step.grid.filemanager;

import java.io.File;

public interface FileManagerClient {

	File requestFile(String uid, long lastModified);

}