package step.core.execution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.model.ReportExport;
import step.core.repositories.RepositoryObjectReference;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExecutionRunnable implements Runnable {
	
	private  final static Logger logger = LoggerFactory.getLogger(ExecutionRunnable.class);

	final ExecutionContext context;
						
	public ExecutionRunnable(ExecutionContext context) {
		super();
		this.context = context;
	}

	public ExecutionContext getContext() {
		return context;
	}
	
	@Override
	public void run() {
		try {
			ExecutionContext.setCurrentContext(context);

			ReportNode rootReportNode = createAndPersistRootReportNode();

			context.getGlobalContext().getExecutionLifecycleManager().executionStarted(this);
			
			updateStatus(ExecutionStatus.IMPORTING);
			String artefactID = importArtefact();
						
			AbstractArtefact artefact = context.getGlobalContext().getArtefactAccessor().get(artefactID);
			context.setArtefact(artefact);
			
			logger.info("Starting test execution. Execution ID: " + context.getExecutionId());
			updateStatus(ExecutionStatus.RUNNING);
						
			ArtefactHandler.delegateCreateReportSkeleton(artefact, rootReportNode);
			ArtefactHandler.delegateExecute(artefact, rootReportNode);

			logger.debug("Test execution ended. Reporting result.... Execution ID: " + context.getExecutionId());

			
			if(!context.isSimulation()) {
				updateStatus(ExecutionStatus.EXPORTING);
				List<ReportExport> exports = exportTestExecutionReport();	
				context.setReportExports(exports);				
				logger.info("Test execution ended and reported. Execution ID: " + context.getExecutionId());
			} else {
				logger.info("Test execution simulation ended. Test report isn't reported in simulation mode. Execution ID: " + context.getExecutionId());
			}
			
		} catch (Throwable e) {
			logger.error("An error occurred while running test. Execution ID: " + context.getExecutionId(), e);
		} finally {
			updateStatus(ExecutionStatus.ENDED);
			context.getGlobalContext().getExecutionLifecycleManager().executionEnded(this);
		}
	}

	private ReportNode createAndPersistRootReportNode() {
		ReportNode resultNode = new ReportNode();
		resultNode.setExecutionID(context.getExecutionId());
		resultNode._id = new ObjectId(context.getExecutionId());
		context.setReport(resultNode);
		context.getReportNodeCache().put(resultNode);
		context.getGlobalContext().getReportAccessor().save(resultNode);
		ExecutionContext.setCurrentReportNode(resultNode);
		return resultNode;
	}
	
	private void updateStatus(ExecutionStatus newStatus) {
		context.getGlobalContext().getExecutionLifecycleManager().updateStatus(this, newStatus);
	}
	
	private String importArtefact() throws Exception {
		String artefactID;
		if(context.getExecutionParameters().getArtefact()!=null) {
			RepositoryObjectReference artefactPointer = context.getExecutionParameters().getArtefact();
			if(artefactPointer!=null) {
				artefactID = context.getGlobalContext().getRepositoryObjectManager().importArtefact(artefactPointer);
			} else {
				throw new Exception("context.artefactID is null and no ArtefactPointer has been specified. This shouldn't happen.");
			}
		} else {
			// TODO
			artefactID = null;
		}
		return artefactID;
	}
	
	private final static String EXPORTS_PARAM = "tec.execution.exports";
	
	@SuppressWarnings("unchecked")
	private List<ReportExport> exportTestExecutionReport() {
		List<ReportExport> exports = new ArrayList<>();
		
		String exportsParam = context.getVariablesManager().getVariableAsString(EXPORTS_PARAM);
		ObjectMapper m = new ObjectMapper();
		m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		JavaType type = m.getTypeFactory().constructCollectionType(List.class, RepositoryObjectReference.class);

		List<RepositoryObjectReference> exportRefs = new ArrayList<>();
		
		try {
			exportRefs.addAll((List<RepositoryObjectReference>) m.readValue(exportsParam, type));
		} catch (IOException e) {
			logger.error("Error occurred while parsing parameter as JSON " + EXPORTS_PARAM, e);
		}
		
		exportRefs.addAll(context.getExecutionParameters().getExports());
		
		logger.info("Exporting test to repositories: " + exportRefs.toString());
		
		for(RepositoryObjectReference reportPointer:exportRefs) {
			ReportExport report = context.getGlobalContext().getRepositoryObjectManager().exportTestExecutionReport(reportPointer, context.getExecutionId());
			exports.add(report);
		}
		return exports;
		
	}

	@Override
	public boolean equals(Object obj) {
		return ((ExecutionRunnable)obj).getContext().getExecutionId().equals(getContext().getExecutionId());
	}
	
	public void cleanUp() {
	}
	
}