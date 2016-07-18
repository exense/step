package step.artefacts.handlers.teststep;

import step.adapters.commons.helper.DocumentTransformer;
import step.artefacts.handlers.teststep.context.Message;
import step.artefacts.handlers.teststep.context.TestStepExecutionContext;
import step.artefacts.reports.TestStepReportNode;
import step.attachments.AttachmentMeta;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.miscellaneous.ReportNodeAttachmentManager;

public class TestStepReportNodeBuilder {
	
	public static void updateTestStepReportNode(TestStepExecutionContext context, TestStepReportNode node) {
		DocumentTransformer transfomer = context.getTransformer();
		
		if(context.getInput()!=null && context.getInput().getPayload()!=null) {
			node.setInput(transfomer.transform(context.getInput().getPayload()));
		}
		
		if(context.getOutput()!=null && context.getOutput().getPayload()!=null) {
			node.setOutput(transfomer.transform(context.getOutput().getPayload()));
		}
		
		if(context.getAdapterToken()!=null) {
			node.setAdapter(context.getAdapterToken().toString());
		}
		
		node.setAttachments(null);
		for(AttachmentMeta attachment:context.getAttachments()) {
			node.addAttachment(attachment);
		}
		
		node.setError(null);
		for(Message message:context.getExceptions()) {
			node.addError(message.getMessage());
			if(message.getCause()!=null) {
				ReportNodeAttachmentManager.attach(message.getCause(), node);
			}
		}
		
		node.setStatus(getReportNodeStatus(context));
	}
	
	private static ReportNodeStatus getReportNodeStatus(TestStepExecutionContext context) {
		if(context.isSkipped()) {
			return ReportNodeStatus.PASSED;
		} else {
			if(context.hasTechnicalError()) {
				return ReportNodeStatus.TECHNICAL_ERROR;				
			} else if(context.hasBusinessError()) {
				return ReportNodeStatus.FAILED;				
			} else {
				return ReportNodeStatus.PASSED;								
			}
		}
	}
}
