package step.core.artefacts.handlers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import step.core.artefacts.AbstractArtefact;

public class ArtefactHandlerRegistry {

	private final Map<Class<? extends AbstractArtefact>, Class<? extends ArtefactHandler<?,?>>> register = new ConcurrentHashMap<>();

	public Class<? extends ArtefactHandler<?, ?>> get(Class<? extends AbstractArtefact> key) {
		return register.get(key);
	}

	public Class<? extends ArtefactHandler<?, ?>> put(Class<? extends AbstractArtefact> key, Class<? extends ArtefactHandler<?, ?>> value) {
		return register.put(key, value);
	}
}
