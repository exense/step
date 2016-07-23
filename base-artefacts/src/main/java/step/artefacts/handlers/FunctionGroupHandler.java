package step.artefacts.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.FunctionGroup;
import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class FunctionGroupHandler extends ArtefactHandler<FunctionGroup, ReportNode> {

	private static final Logger logger = LoggerFactory.getLogger(FunctionGroupHandler.class);
	
	public static final String ADAPTER_SESSION_PARAM_KEY = "##session##";

	@Override
	protected void createReportSkeleton_(ReportNode node, FunctionGroup testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.createReportSkeleton_(node, testArtefact);
	}

	@Override
	protected void execute_(ReportNode node, FunctionGroup testArtefact) {
//		String sessionID = UUID.randomUUID().toString();
//		AdapterSession adapterSession = new AdapterSession(sessionID);
//
//		context.getVariablesManager().putVariable(node, ADAPTER_SESSION_PARAM_KEY, adapterSession);
//				
//		logger.debug("Created new adapter session " + sessionID);
//		
//		
//		try {
//			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
//			scheduler.execute_(node, testArtefact);
//		} finally {
//			AdapterClient adapterClient = (AdapterClient) ExecutionContext.getCurrentContext().getGlobalContext().get(GridPlugin.GRIDCLIENT_KEY);
//			adapterClient.releaseSession(adapterSession);
//		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, FunctionGroup testArtefact) {
		return new ReportNode();
	}
}
