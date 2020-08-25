package step.core.artefacts;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The only reason why this class exist and has been implemented in a static way is the {@link ArtefactTypeIdResolver}.
 * This class SHOULDN'T be used by any other class.
 */
public class ArtefactTypeCache {
	
	private static final ArtefactTypeCache INSTANCE = new ArtefactTypeCache();
	private final Map<String, Class<? extends AbstractArtefact>> artefactRegister = new ConcurrentHashMap<>();

	public static Class<? extends AbstractArtefact> put(String key, Class<? extends AbstractArtefact> value) {
		return INSTANCE.artefactRegister.put(key, value);
	}

	public static Class<? extends AbstractArtefact> getArtefactType(String name) {
		return INSTANCE.artefactRegister.get(name);
	}

	public static Set<String> keySet() {
		return INSTANCE.artefactRegister.keySet();
	}
}
