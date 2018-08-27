package step.core.execution;

import org.bson.types.ObjectId;

import step.attachments.AttachmentManager;
import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.core.accessors.CollectionRegistry;
import step.core.artefacts.InMemoryArtefactAccessor;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plugins.PluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.expressions.ExpressionHandler;

public class ContextBuilder {
	
	public static ExecutionContext createLocalExecutionContext() {
		
		GlobalContext g = createGlobalContext();
		
		ExecutionContext c = createContext(g);

		return c;
	}

	public static ExecutionContext createContext(GlobalContext g) {
		ReportNode root = new ReportNode();
		ExecutionContext c = new ExecutionContext(new ObjectId().toString());
		c.setGlobalContext(g);
		c.getReportNodeCache().put(root);
		c.setReport(root);
		c.setCurrentReportNode(root);
		c.setExecutionParameters(new ExecutionParameters("dummy", null, ExecutionMode.RUN));
		return c;
	}
	
	public static GlobalContext createGlobalContext() {
		GlobalContext context = new GlobalContext();

		context.setExpressionHandler(new ExpressionHandler());
		context.setDynamicBeanResolver(new DynamicBeanResolver(new DynamicValueResolver(context.getExpressionHandler())));
		
		PluginManager pluginManager = new PluginManager();
		context.setPluginManager(pluginManager);
		
		//MongoClient client = Mockito.mock(MongoClient.class);
		//context.setMongoClient(client);
		
		context.setConfiguration(Configuration.getInstance());
		
		context.put(CollectionRegistry.class, new CollectionRegistry());
		context.setExecutionAccessor(new InMemoryExecutionAccessor());
		context.setArtefactAccessor(new InMemoryArtefactAccessor());
		context.setReportAccessor(new InMemoryReportNodeAccessor());
		
		//ExecutionTaskAccessor schedulerAccessor = Mockito.mock(ExecutionTaskAccessor.class);
		//context.setScheduleAccessor(schedulerAccessor);
		context.setRepositoryObjectManager(new RepositoryObjectManager(context.getArtefactAccessor()));
		
		context.setEventManager(new EventManager());
		context.setAttachmentManager(new AttachmentManager(Configuration.getInstance()));

		
		return context;
	}
}
