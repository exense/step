package step.core.deployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicateFactory;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;

@Plugin
public class ObjectHookControllerPlugin extends AbstractControllerPlugin {

	private ObjectHookRegistry objectHookRegistry;
	
	static final Logger logger = LoggerFactory.getLogger(ObjectHookControllerPlugin.class);

	@Override
	public void executionControllerStart(GlobalContext context) {
		objectHookRegistry = new ObjectHookRegistry();
		context.put(ObjectHookRegistry.class, objectHookRegistry);
		
		ObjectPredicateFactory objectPredicateFactory = new ObjectPredicateFactory(objectHookRegistry);
		context.put(ObjectPredicateFactory.class, objectPredicateFactory);
		
		context.getServiceRegistrationCallback().registerService(ObjectHookInterceptor.class);
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		return new ObjectHookPlugin(objectHookRegistry);
	}

}
