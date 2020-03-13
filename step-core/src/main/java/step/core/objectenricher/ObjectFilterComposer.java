package step.core.objectenricher;

import java.util.Iterator;
import java.util.List;

public class ObjectFilterComposer {

	public static ObjectFilter compose(List<ObjectFilter> list) {
		return new ObjectFilter() {
			@Override
			public String getOQLFilter() {
				String filter = "";
				Iterator<ObjectFilter> iterator = list.iterator();
				while(iterator.hasNext()) {
					filter += iterator.next().getOQLFilter();
					if(iterator.hasNext()) {
						filter += " and ";
					}
				}
				return filter;
			}
		};
	}
}
