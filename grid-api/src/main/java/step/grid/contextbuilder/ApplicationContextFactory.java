package step.grid.contextbuilder;

public abstract class ApplicationContextFactory {
	
	public abstract String getId();
	
	public abstract boolean requiresReload();
	
	public abstract ClassLoader buildClassLoader(ClassLoader parentClassLoader);
	
}
