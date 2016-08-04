package step.core.miscellaneous;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentContainer;
import step.attachments.AttachmentManager;
import step.attachments.AttachmentMeta;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.variables.UndefinedVariableException;
import step.core.variables.VariablesManager;



public class ReportNodeAttachmentManager {

	public static String QUOTA_COUNT_VARNAME = "tec.quota.attachments.count";
	
	public static String QUOTA_VARNAME = "tec.quota.attachments";
	
	private static final Logger logger = LoggerFactory.getLogger(ReportNodeAttachmentManager.class);
	
	private static boolean checkAndUpateAttachmentQuota() {
		ExecutionContext context = ExecutionContext.getCurrentContext();
		synchronized (context) {
			VariablesManager varManager = context.getVariablesManager();
			
			Integer count;
			try {
				count = varManager.getVariableAsInteger(QUOTA_COUNT_VARNAME);
				varManager.updateVariable(QUOTA_COUNT_VARNAME, count+1); 
			} catch (UndefinedVariableException e) {
				count = 1;
				varManager.putVariable(context.getReportNodeTree().getRoot(), QUOTA_COUNT_VARNAME, count);				
			}
			
			Integer quota;
			try {
				quota = varManager.getVariableAsInteger(QUOTA_VARNAME);
			} catch (UndefinedVariableException e) {
				quota = 100;
			}
			
			if(quota==count) {
				logger.info(ExecutionContext.getCurrentContext().getExecutionId().toString() + ". Maximum number of attachment (" +quota+") reached. Next attachments will be skipped.");
			}
			
			return quota>=count;
		}		
	}
	
	public static class AttachmentQuotaException extends Exception {

		private static final long serialVersionUID = 4089543269056812252L;

		public AttachmentQuotaException(String message) {
			super(message);
		}
	}
	
	private static byte[] exceptionToAttachment(Throwable e) {
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		return w.toString().getBytes();
	}
	
	public static AttachmentMeta createAttachment(Throwable e) throws AttachmentQuotaException {
		return createAttachment(exceptionToAttachment(e), "exception.log");
	}
	
	public static void attach(Throwable e, ReportNode node) {
		attach(exceptionToAttachment(e), "exception.log", node);
	}
	
	public static AttachmentMeta createAttachment(byte[] content, String filename) throws AttachmentQuotaException {
		if(checkAndUpateAttachmentQuota()) {
			AttachmentContainer container = AttachmentManager.createAttachmentContainer();
			try {
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(container.getContainer().getAbsoluteFile()+"/"+filename)));
				bos.write(content);
				bos.close();
			} catch (IOException ex) {
				logger.error("Unable to write exception.log", ex);
			}
			
			AttachmentMeta attachment = container.getMeta();
			attachment.setName(filename);
			return attachment;
		} else {
			logger.debug(ExecutionContext.getCurrentContext().getExecutionId().toString() + ". Skipping attachment \"" + filename + "\"");
			throw new AttachmentQuotaException("The attachment " + filename + " has been skipped because the test " +
					"generated more than the maximum number of attachments permitted.");
		}
	}
	
	public static void attach(byte[] content, String filename, ReportNode reportNode ) {
		try {
			AttachmentMeta attachment = createAttachment(content, filename);
			reportNode.addAttachment(attachment);
		} catch (AttachmentQuotaException e) {
			logger.debug(ExecutionContext.getCurrentContext().getExecutionId().toString() + ". Skipping attachment \"" + filename + "\"");
			reportNode.addError("The attachment " + filename + " has been skipped because the test "
					+ "generated more than the maximum number of attachments permitted.");
		}
	}
	

}
