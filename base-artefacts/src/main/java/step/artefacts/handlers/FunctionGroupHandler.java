package step.artefacts.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.FunctionGroup;
import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.functions.FunctionClient;
import step.functions.FunctionClient.FunctionToken;
import step.grid.tokenpool.Interest;
import step.plugins.adaptergrid.GridPlugin;

public class FunctionGroupHandler extends ArtefactHandler<FunctionGroup, ReportNode> {

	private static final Logger logger = LoggerFactory.getLogger(FunctionGroupHandler.class);
	
	public static final String TOKEN_PARAM_KEY = "##token##";

	@Override
	protected void createReportSkeleton_(ReportNode node, FunctionGroup testArtefact) {
		SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
		scheduler.createReportSkeleton_(node, testArtefact);
	}

	@Override
	protected void execute_(ReportNode node, FunctionGroup testArtefact) {
		FunctionClient functionClient = (FunctionClient) ExecutionContext.getCurrentContext().getGlobalContext().get(GridPlugin.FUNCTIONCLIENT_KEY);

		Map<String, Interest> interests = new HashMap<>();
		if(testArtefact.getSelectionCriteria()!=null) {
			testArtefact.getSelectionCriteria().forEach((e,v)->interests.put(e, new Interest(Pattern.compile(v), true)));
		}
		FunctionToken token = functionClient.getFunctionToken(testArtefact.getAttributes(), interests);
		context.getVariablesManager().putVariable(node, TOKEN_PARAM_KEY, token);
		
		try {
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
			scheduler.execute_(node, testArtefact);
		} finally {
			token.release();
		}	
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, FunctionGroup testArtefact) {
		return new ReportNode();
	}
}
