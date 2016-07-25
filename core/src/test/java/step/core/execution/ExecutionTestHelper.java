package step.core.execution;

import org.mockito.Mockito;

import step.core.GlobalContext;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.plugins.PluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionTaskAccessor;

import com.mongodb.MongoClient;

public class ExecutionTestHelper {

	public static void setupContext() {
		ExecutionContext c = createContext();
		
		c.getVariablesManager().putVariable(c.getReport(), ReportNodeAttachmentManager.QUOTA_VARNAME, 100);
		c.getVariablesManager().putVariable(c.getReport(), ArtefactHandler.CONTINUE_EXECUTION, "false");
		
		ExecutionContext.setCurrentContext(c);
		
	}
	
	public static ExecutionContext createContext() {
		
		GlobalContext g = createGlobalContext();
		
		ExecutionContext c = createContext(g);

		return c;
	}

	public static ExecutionContext createContext(GlobalContext g) {
		ReportNode root = new ReportNode();
		ExecutionContext c = new ExecutionContext("");
		c.setGlobalContext(g);
		c.getReportNodeCache().put(root);
		c.setReport(root);
		ExecutionContext.setCurrentReportNode(root);
		c.setExecutionParameters(new ExecutionParameters("dummy", null, ExecutionMode.RUN));
		return c;
	}
	
	public static GlobalContext createGlobalContext() {
		GlobalContext context = new GlobalContext();

		PluginManager pluginManager = new PluginManager();
		context.setPluginManager(pluginManager);
		
		MongoClient client = Mockito.mock(MongoClient.class);
		context.setMongoClient(client);
		
		context.setExecutionAccessor(new InMemoryExecutionAccessor());
		context.setArtefactAccessor(new InMemoryArtefactAccessor());
		context.setReportAccessor(new InMemoryReportNodeAccessor());
		
		ExecutionTaskAccessor schedulerAccessor = Mockito.mock(ExecutionTaskAccessor.class);
		context.setScheduleAccessor(schedulerAccessor);
		context.setRepositoryObjectManager(new RepositoryObjectManager(context.getArtefactAccessor()));
		context.setExecutionLifecycleManager(new ExecutionLifecycleManager(context));
		
		return context;
	}
}
