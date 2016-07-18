package step.artefacts.handlers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.Session;
import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.grid.client.AdapterClient;
import step.grid.client.AdapterSession;
import step.plugins.adaptergrid.AdapterClientPlugin;

public class SessionHandler extends ArtefactHandler<Session, ReportNode> {

	private static final Logger logger = LoggerFactory.getLogger(SessionHandler.class);
	
	public static final String ADAPTER_SESSION_PARAM_KEY = "##session##";

	@Override
	protected void createReportSkeleton_(ReportNode node, Session testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.createReportSkeleton_(node, testArtefact);
	}

	@Override
	protected void execute_(ReportNode node, Session testArtefact) {
		String sessionID = UUID.randomUUID().toString();
		AdapterSession adapterSession = new AdapterSession(sessionID);

		context.getVariablesManager().putVariable(node, ADAPTER_SESSION_PARAM_KEY, adapterSession);
				
		logger.debug("Created new adapter session " + sessionID);
		
		
		try {
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
			scheduler.execute_(node, testArtefact);
		} finally {
			AdapterClient adapterClient = (AdapterClient) ExecutionContext.getCurrentContext().getGlobalContext().get(AdapterClientPlugin.ADAPTER_CLIENT_KEY);
			adapterClient.releaseSession(adapterSession);
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Session testArtefact) {
		return new ReportNode();
	}
}
