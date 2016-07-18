package step.localadapters;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import step.adapters.commons.model.Input;
import step.adapters.commons.model.Output;
import step.adapters.commons.model.OutputBuilder;
import step.artefacts.handlers.teststep.AbstractLocalAdapter;
import step.core.execution.ExecutionContext;

public class GetFromQueueAdapter extends AbstractLocalAdapter {

	@Override
	public Output executeAdapter(Input input) throws Exception {
		// TODO get QUeueManager from context
		QueueManager queueManager = null;
		
		String queueName = input.getPayload().getDocumentElement().getAttribute("Queue");
		String timeoutStr = input.getPayload().getDocumentElement().getAttribute("Timeout");
		String itemName = input.getPayload().getDocumentElement().getAttribute("Item");
		long timeout = Long.decode(timeoutStr);
		
		OutputBuilder outputBuilder = new OutputBuilder();
		try {
			Map<String, Object> value = queueManager.getFromQueue(queueName, timeout);
			ExecutionContext ctx = ExecutionContext.getCurrentContext();
			ctx.getVariablesManager().putVariable(ctx.getReportNodeTree().getParent(ExecutionContext.getCurrentReportNode()), itemName, value);;
			outputBuilder.setPayload("<Return />");
		} catch (InterruptedException | TimeoutException e) {
			outputBuilder.setTechnicalError("Timeout occurred while wating for queue " + queueName);
		}
		
		return outputBuilder.build();
	}

}
