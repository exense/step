package step.core.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import step.core.objectenricher.ObjectEnricher;

public class ObjectEnrichers {

	private final List<Function<Session, ObjectEnricher>> objectEnrichers = new ArrayList<>();

	/**
	 * Registers a new factory for {@link ObjectEnricher}
	 * @param objectEnricherFactory the factory to be registered
	 */
	public void register(Function<Session, ObjectEnricher> objectEnricherFactory) {
		objectEnrichers.add(objectEnricherFactory);
	}
	
	/**
	 * Builds a new {@link ObjectEnricher} based on the registered factories for the provided session
	 * @param session
	 * @return
	 */
	public ObjectEnricher getObjectEnricher(Session session) {
		List<ObjectEnricher> enrichersForSession = objectEnrichers.stream().map(f->f.apply(session)).collect(Collectors.toList());
		return new ObjectEnricher() {
			
			@Override
			public void accept(Object o) {
				enrichersForSession.forEach(enricher->enricher.accept(o));
			}
			
			@Override
			public Map<String, String> getAdditionalAttributes() {
				HashMap<String, String> attributes = new HashMap<String, String>();
				enrichersForSession.forEach(enricher->attributes.putAll(enricher.getAdditionalAttributes()));
				return attributes;
			}
		};
	}
}
