package step.resources;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceRevisionContent {

	InputStream getResourceStream();

	String getResourceName();

	void close() throws IOException;

}