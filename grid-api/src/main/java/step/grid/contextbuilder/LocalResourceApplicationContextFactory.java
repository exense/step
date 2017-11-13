package step.grid.contextbuilder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import step.grid.bootstrap.ResourceExtractor;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerClient.FileVersion;

public class LocalResourceApplicationContextFactory extends ApplicationContextFactory {

	String resourceName;
	
	ClassLoader resourceClassLoader;
	
	protected FileManagerClient fileManager;
	
	FileVersion localClassLoaderFolder;
	
	public LocalResourceApplicationContextFactory(ClassLoader resourceClassLoader, String resourceName) {
		super();
		this.resourceName = resourceName;
		this.resourceClassLoader = resourceClassLoader;
	}

	@Override
	public String getId() {
		return resourceName;
	}

	@Override
	public boolean requiresReload() {
		return false;
	}

	@Override
	public ClassLoader buildClassLoader(ClassLoader parentClassLoader) {
		File jar = ResourceExtractor.extractResource(resourceClassLoader, resourceName);
		List<URL> urls = ClassPathHelper.forSingleFile(jar);
		URL[] urlArray = urls.toArray(new URL[urls.size()]);
		URLClassLoader cl = new URLClassLoader(urlArray, parentClassLoader);
		return cl;	
	}

}
