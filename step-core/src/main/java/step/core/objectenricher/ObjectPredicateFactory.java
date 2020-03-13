package step.core.objectenricher;

import step.core.AbstractContext;
import step.core.ql.Filter;
import step.core.ql.OQLFilterBuilder;

public class ObjectPredicateFactory {

	private final ObjectHookRegistry objectHookRegistry;

	public ObjectPredicateFactory(ObjectHookRegistry objectHookRegistry) {
		super();
		this.objectHookRegistry = objectHookRegistry;
	}
	
	public ObjectPredicate getObjectPredicate(AbstractContext context) {
		ObjectFilter objectFilter = objectHookRegistry.getObjectFilter(context);
		String oqlFilter = objectFilter.getOQLFilter();
		Filter<Object> filter = OQLFilterBuilder.getFilter(oqlFilter);
		return new ObjectPredicate() {
			@Override
			public boolean test(Object t) {
				return filter.test(t);
			}
		};
	}
}
