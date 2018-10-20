package step.grid.contextbuilder;

import step.grid.filemanager.FileProviderException;

public abstract class ApplicationContextFactory {
	
	public abstract String getId();
	
	public abstract boolean requiresReload() throws FileProviderException;
	
	public abstract ClassLoader buildClassLoader(ClassLoader parentClassLoader) throws FileProviderException;
	
}
