package step.resources;

import java.io.File;
import java.io.IOException;

public interface ResourceRevisionFileHandle {

	File getResourceFile();

	void close() throws IOException;

}