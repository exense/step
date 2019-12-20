package step.core.objectenricher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectFilterComposer {

	public static ObjectFilter compose(List<ObjectFilter> list) {
		return new ObjectFilter() {
			
			@Override
			public boolean test(Object t) {
				for (ObjectFilter objectFilter : list) {
					if(!objectFilter.test(t)) {
						return false;
					}
				}
				return true;
			}
			
			@Override
			public Map<String, String> getAdditionalAttributes() {
				Map<String, String> result = new HashMap<>();
				list.forEach(f->result.putAll(f.getAdditionalAttributes()));
				return result;
			}
		};
	}
}
