package step.grid.contextbuilder;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerClient.FileVersion;
import step.grid.filemanager.FileManagerClient.FileVersionId;

public class RemoteApplicationContextFactory extends ApplicationContextFactory {

	protected FileVersionId remoteClassLoaderFolder;
	
	protected FileManagerClient fileManager;

	public RemoteApplicationContextFactory(FileManagerClient fileManager, FileVersionId remoteClassLoaderFolder) {
		super();
		this.fileManager = fileManager;
		this.remoteClassLoaderFolder = remoteClassLoaderFolder;
	}

	@Override
	public String getId() {
		return remoteClassLoaderFolder.getFileId();
	}

	@Override
	public boolean requiresReload() {
		FileVersion localClassLoaderFolder = requestLatestClassPathFolder();
		return localClassLoaderFolder.isModified();
	}

	private FileVersion requestLatestClassPathFolder() {
		try {
			return fileManager.requestFileVersion(remoteClassLoaderFolder.getFileId(), remoteClassLoaderFolder.getVersion());			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ClassLoader buildClassLoader(ClassLoader parentClassLoader) {
		FileVersion localClassLoaderFolder = requestLatestClassPathFolder();

		List<URL> urls;
		if (localClassLoaderFolder.getFile().isDirectory()) {
			urls = ClassPathHelper.forAllJarsInFolder(localClassLoaderFolder.getFile());
		} else {
			urls = ClassPathHelper.forSingleFile(localClassLoaderFolder.getFile());
		}	
		
		URL[] urlArray = urls.toArray(new URL[urls.size()]);
		ClassLoader classLoader = new URLClassLoader(urlArray, parentClassLoader);
		return classLoader;
	}

}
