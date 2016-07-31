package step.artefacts.handlers;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;

import step.artefacts.Return;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.grid.io.OutputMessage;

public class ReturnHandler extends ArtefactHandler<Return, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Return testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Return testArtefact) {
		node.setStatus(ReportNodeStatus.PASSED);

		Object o = context.getVariablesManager().getVariable("output");
		if(o!=null && o instanceof OutputMessage) {
			JsonObject payload = Json.createReader(new StringReader(testArtefact.getValue())).readObject();
			((OutputMessage)o).setPayload(payload);
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Return testArtefact) {
		return new ReportNode();
	}
}
