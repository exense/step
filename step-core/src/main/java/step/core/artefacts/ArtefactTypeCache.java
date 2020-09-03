package step.core.artefacts;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import step.core.scanner.AnnotationScanner;

/**
 * The only reason why this class exists and has been implemented in a static
 * way is that {@link ArtefactTypeIdResolver} cannot be provided with a context
 * object. This class SHOULDN'T be used by any other class and should be removed
 * as soon as a better solution can be found.
 */
@SuppressWarnings("unchecked")
public class ArtefactTypeCache {
	
	private static final ArtefactTypeCache INSTANCE = new ArtefactTypeCache();
	
	private final Map<String, Class<? extends AbstractArtefact>> artefactRegister;
	
	/**
	 * Calling {@link AbstractArtefact#getArtefactName} is quite inefficient because it relies on
	 * {@link Class#getSimpleName()}. We're therefore using a cache for artefact names here
	 */
	private final Map<Class<? extends AbstractArtefact>, String> artefactNameCache;

	{
		artefactRegister = new ConcurrentHashMap<>();
		artefactNameCache = new ConcurrentHashMap<>();
		
		Set<Class<?>> artefactClasses = AnnotationScanner.getClassesWithAnnotation(Artefact.class);
		for (Class<?> artefactClass_ : artefactClasses) {
			Class<? extends AbstractArtefact> artefactClass = (Class<? extends AbstractArtefact>) artefactClass_; 
			String artefactName = AbstractArtefact.getArtefactName((Class<AbstractArtefact>)artefactClass);
			artefactRegister.put(artefactName, artefactClass);
			artefactNameCache.put(artefactClass, artefactName);
		}
	}
	
	public static Class<? extends AbstractArtefact> getArtefactType(String name) {
		return INSTANCE.artefactRegister.get(name);
	}
	
	public static String getArtefactName(Class<? extends AbstractArtefact> artefactClass) {
		return INSTANCE.artefactNameCache.get(artefactClass);
	}

	public static Set<String> keySet() {
		return INSTANCE.artefactRegister.keySet();
	}
}
