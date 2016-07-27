package step.core.tokenhandlers;

import java.io.StringReader;

import javax.json.Json;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ArtefactMessageHandler implements MessageHandler {

	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		ExecutionContext executionContext = ExecutionContext.getCurrentContext();
		GlobalContext globalContext = executionContext.getGlobalContext();
		
		
		String artefactId = message.getArgument().getString("artefactid");
		String parentReportId = null;
		
		ReportNode parentNode;
		if(parentReportId == null) {
			parentNode = new ReportNode();
			globalContext.getReportAccessor().save(parentNode);
			parentReportId = parentNode.getId().toString();
		} else {
			parentNode = globalContext.getReportAccessor().get(new ObjectId(parentReportId));
		}
		
		AbstractArtefact artefact = globalContext.getArtefactAccessor().get(artefactId);
		
		ReportNode node = ArtefactHandler.delegateExecute(artefact,parentNode);

		OutputMessage output = new OutputMessage();
		
		output.setError(node.getError());
		
		ObjectMapper m = new ObjectMapper();
		output.setPayload(Json.createReader(new StringReader(m.writeValueAsString(node))).readObject());
		return output;
	}

}
