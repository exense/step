package step.core.artefacts;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

public class ArtefactRegistry {
	
	private static ArtefactRegistry instance;

	@SuppressWarnings("unchecked")
	public static synchronized ArtefactRegistry getInstance() {
		if(instance == null) {
			instance = new ArtefactRegistry();
			
			Set<Class<?>> artefacts = new Reflections("step.artefacts").getTypesAnnotatedWith(Artefact.class);
			
			for(Class<?> artfefactClass:artefacts) {
				instance.register((Class<? extends AbstractArtefact>) artfefactClass);
			}
		}
		
		return instance;
	}

	Map<String, Class<? extends AbstractArtefact>> register = new HashMap<>();
	
	public void register(Class<? extends AbstractArtefact> artefact) {
		register.put(getArtefactName(artefact), artefact);
	}
	
	public Set<String> getArtefactNames() {
		return register.keySet();
	}
	
	public Class<? extends AbstractArtefact> getArtefactType(String name) {
		return register.get(name);
	}
	
	public static String getArtefactName(Class<? extends AbstractArtefact> artefactClass) {
		Artefact annotation = artefactClass.getAnnotation(Artefact.class);
		return annotation.name().length()>0?annotation.name():artefactClass.getSimpleName();
	}
	
}
