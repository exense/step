package step.core.deployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectFilter;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectPredicateFactory;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.ql.Filter;
import step.core.ql.OQLFilterBuilder;

@Plugin
public class ObjectHookPlugin extends AbstractControllerPlugin {

	private ObjectHookRegistry objectHookRegistry;
	
	private static final Logger logger = LoggerFactory.getLogger(ObjectHookPlugin.class);

	@Override
	public void executionControllerStart(GlobalContext context) {
		objectHookRegistry = new ObjectHookRegistry();
		context.put(ObjectHookRegistry.class, objectHookRegistry);
		
		ObjectPredicateFactory objectPredicateFactory = new ObjectPredicateFactory(objectHookRegistry);
		context.put(ObjectPredicateFactory.class, objectPredicateFactory);
		
		context.getServiceRegistrationCallback().registerService(ObjectHookInterceptor.class);
	}

	@Override
	public void executionStart(ExecutionContext executionContext) {
		// Rebuild the session based on the ExecutionParameters of the execution context
		// This has to be done because the Session is not always available when running an execution
		// (by Scheduled tasks for instance) 
		Session session = new Session();
		objectHookRegistry.forEach(hook->{
			try {
				hook.rebuildContext(session, executionContext.getExecutionParameters());
			} catch (Exception e) {
				logger.error("Error while rebuilding context for execution "+executionContext.getExecutionId(), e);
			}
		});
		
		// Add the composed object enricher and filters to the execution context
		executionContext.put(ObjectEnricher.class, objectHookRegistry.getObjectEnricher(session));
		ObjectFilter objectFilter = objectHookRegistry.getObjectFilter(session);
		executionContext.put(ObjectFilter.class, objectFilter);
		
		Filter<Object> filter = OQLFilterBuilder.getFilter(objectFilter.getOQLFilter());
		executionContext.put(ObjectPredicate.class, new ObjectPredicate() {
			@Override
			public boolean test(Object t) {
				return filter.test(t);
			}
		});
	}
}
