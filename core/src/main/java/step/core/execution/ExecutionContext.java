package step.core.execution;

import java.util.List;

import step.core.AbstractContext;
import step.core.GlobalContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.model.ReportExport;
import step.core.variables.VariablesManager;

public class ExecutionContext extends AbstractContext  {
	
	private static ThreadLocal<ExecutionContext> contextRegistry = new ThreadLocal<>();
	
	private static ThreadLocal<ReportNode> currentNodeRegistry = new ThreadLocal<>();
	
	private GlobalContext globalContext;

	private ExecutionParameters executionParameters;
	
	private final String executionId;
	
	private String executionTaskID;
					
	private AbstractArtefact artefact;

	private ReportNode report;
	
	private ExecutionStatus status;
	
	private final VariablesManager variablesManager;
				
	private final ReportNodeCache reportNodeCache;
	
	private final ReportNodeTree reportNodeTree;
	
	private List<ReportExport> reportExports;

	public ExecutionContext(String executionId) {
		super();
				
		this.executionId = executionId;
				
		reportNodeCache = new ReportNodeCache();
		reportNodeTree = new ReportNodeTree(this);
				
		variablesManager = new VariablesManager(this);
	}

	public AbstractArtefact getArtefact() {
		return artefact;
	}

	public void setArtefact(AbstractArtefact artefact) {
		this.artefact = artefact;
	}

	public ReportNode getReport() {
		return report;
	}

	public void setReport(ReportNode report) {
		this.report = report;
	}

	public String getExecutionTaskID() {
		return executionTaskID;
	}

	public void setExecutionTaskID(String executionTaskID) {
		this.executionTaskID = executionTaskID;
	}

	public static ExecutionContext getCurrentContext() {
		ExecutionContext context = contextRegistry.get();
		if(context==null) {
			throw new RuntimeException("ExecutionContext is null! Ensure that the thread has been linked to the ExecutionContext " +
					"by calling 'ExecutionContext.setCurrentContext(context)'");
		}
		return context;
	}
	
	public ReportNodeCache getReportNodeCache() {
		return reportNodeCache;
	}

	public static ReportNode getCurrentReportNode() {
		ReportNode currentNode = currentNodeRegistry.get();
		if(currentNode==null) {
			throw new RuntimeException("Current report node is null!");
		} else {
			return currentNode;
		}
	}
	
	public static void setCurrentReportNode(ReportNode node) {
		currentNodeRegistry.set(node);
	}
	
	public static void setCurrentContext(ExecutionContext context) {
		contextRegistry.set(context);
		context.getGlobalContext().getPluginManager().getProxy().associateThread(context, Thread.currentThread());
	}

	public String getExecutionId() {
		return executionId;
	}

	public ExecutionStatus getStatus() {
		return status;
	}
	
	public boolean isInterrupted() {
		return getStatus() == ExecutionStatus.ABORTING;
	}

	public boolean isSimulation() {
		return getExecutionParameters().getMode()==ExecutionMode.SIMULATION;
	}
	
	public void updateStatus(ExecutionStatus status) {
		this.status = status;
	}
	
	public VariablesManager getVariablesManager() {
		return variablesManager;
	}

	@Override
	public String toString() {
		return "ExecutionContext [executionId=" + executionId + "]";
	}

	public List<ReportExport> getReportExports() {
		return reportExports;
	}

	public void setReportExports(List<ReportExport> reportExports) {
		this.reportExports = reportExports;
	}

	public ExecutionParameters getExecutionParameters() {
		return executionParameters;
	}

	public void setExecutionParameters(ExecutionParameters parameters) {
		this.executionParameters = parameters;
	}
	
	public GlobalContext getGlobalContext() {
		return globalContext;
	}

	public void setGlobalContext(GlobalContext globalContext) {
		this.globalContext = globalContext;
	}

	public ReportNodeTree getReportNodeTree() {
		return reportNodeTree;
	}
}
