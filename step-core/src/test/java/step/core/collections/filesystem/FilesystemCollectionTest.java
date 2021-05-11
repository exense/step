package step.core.collections.filesystem;

import java.io.File;
import java.io.IOException;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import step.core.collections.AbstractCollectionTest;

public class FilesystemCollectionTest extends AbstractCollectionTest {

	public FilesystemCollectionTest() throws IOException {
		super(new FilesystemCollectionFactory(getConfiguration()));
	}
	
	private static Configuration getConfiguration() throws IOException {
		File folder = FileHelper.createTempFolder();
		Configuration configuration = new Configuration();
		configuration.putProperty(FilesystemCollectionFactory.DB_FILESYSTEM_PATH, folder.getAbsolutePath());
		return configuration;
	}
}
