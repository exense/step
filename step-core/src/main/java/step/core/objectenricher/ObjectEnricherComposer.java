package step.core.objectenricher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectEnricherComposer {

	public static ObjectEnricher compose(List<ObjectEnricher> list) {
		return new ObjectEnricher() {

			@Override
			public void accept(Object o) {
				list.forEach(enricher -> enricher.accept(o));
			}

			@Override
			public Map<String, String> getAdditionalAttributes() {
				HashMap<String, String> attributes = new HashMap<String, String>();
				list.forEach(enricher -> attributes.putAll(enricher.getAdditionalAttributes()));
				return attributes;
			}
		};
	}

}
