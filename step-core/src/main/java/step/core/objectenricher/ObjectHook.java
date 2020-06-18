package step.core.objectenricher;

import step.core.AbstractContext;

/**
 * An {@link ObjectHook} is a factory for
 * {@link ObjectFilter} and {@link ObjectEnricher}
 *
 */
public interface ObjectHook {

	public ObjectFilter getObjectFilter(AbstractContext context);
	
	public ObjectEnricher getObjectEnricher(AbstractContext context);
	
	public ObjectEnricher getObjectDrainer(AbstractContext context);
	
	/**
	 * Rebuilds an {@link AbstractContext} based on an object that has been
	 * previously enriched with an {@link ObjectEnricher} provided by this class
	 * 
	 * @param context the context to be recreated
	 * @param object the object to base the context reconstruction on
	 * @throws Exception
	 */
	public void rebuildContext(AbstractContext context, Object object) throws Exception;
}
