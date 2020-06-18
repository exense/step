package step.core.objectenricher;

import java.util.ArrayList;
import java.util.stream.Collectors;

import step.core.AbstractContext;

@SuppressWarnings("serial")
public class ObjectHookRegistry extends ArrayList<ObjectHook> {

	/**
	 * @param context
	 * @return the composed {@link ObjectFilter} based on all the registered hooks
	 */
	public ObjectFilter getObjectFilter(AbstractContext context) {
		return ObjectFilterComposer
				.compose(stream().map(hook -> hook.getObjectFilter(context)).collect(Collectors.toList()));
	}

	/**
	 * @param context
	 * @return the composed {@link ObjectEnricher} based on all the registered hooks
	 */
	public ObjectEnricher getObjectEnricher(AbstractContext context) {
		return ObjectEnricherComposer
				.compose(stream().map(hook -> hook.getObjectEnricher(context)).collect(Collectors.toList()));
	}
	
	/**
	 * @param context
	 * @return the composed {@link ObjectEnricher} based on all the registered hooks
	 */
	public ObjectEnricher getObjectDrainer(AbstractContext context) {
		return ObjectEnricherComposer
				.compose(stream().map(hook -> hook.getObjectDrainer(context)).collect(Collectors.toList()));
	}

}
