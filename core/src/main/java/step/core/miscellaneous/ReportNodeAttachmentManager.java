/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.core.miscellaneous;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentMeta;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.variables.UndefinedVariableException;
import step.core.variables.VariablesManager;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;


// TODO refactor this class to remove the ExecutionContext dependency 
public class ReportNodeAttachmentManager {

	public static String QUOTA_COUNT_VARNAME = "tec.quota.attachments.count";
	
	public static String QUOTA_VARNAME = "tec.quota.attachments";
	
	private static final Logger logger = LoggerFactory.getLogger(ReportNodeAttachmentManager.class);
	
	private ResourceManager resourceManager;
	
	private ExecutionContext context;
	
	public ReportNodeAttachmentManager(ExecutionContext context) {
		super();
		this.context = context;
		this.resourceManager = context.get(ResourceManager.class);
	}

	// Warning: only use this constructor if you know that you will only use the method createAttachmentWithoutQuotaCheck. 
	// The other methods need an ExecutionContext to check the quota usage
	public ReportNodeAttachmentManager(ResourceManager resourceManager) {
		super();
		this.resourceManager = resourceManager;
	}

	private boolean checkAndUpateAttachmentQuota() {
		synchronized (context) {
			VariablesManager varManager = context.getVariablesManager();
			
			Integer count;
			try {
				count = varManager.getVariableAsInteger(QUOTA_COUNT_VARNAME);
				varManager.updateVariable(QUOTA_COUNT_VARNAME, count+1); 
			} catch (UndefinedVariableException e) {
				count = 1;
				varManager.putVariable(context.getReport(), QUOTA_COUNT_VARNAME, count);				
			}
			
			Integer quota;
			try {
				quota = varManager.getVariableAsInteger(QUOTA_VARNAME);
			} catch (UndefinedVariableException e) {
				quota = 100;
			}
			
			if(quota==count) {
				logger.info(context.getExecutionId().toString() + ". Maximum number of attachment (" +quota+") reached. Next attachments will be skipped.");
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
	
	public AttachmentMeta createAttachment(Throwable e) throws AttachmentQuotaException {
		return createAttachment(exceptionToAttachment(e), "exception.log");
	}
	
	public void attach(Throwable e, ReportNode node) {
		attach(exceptionToAttachment(e), "exception.log", node);
	}
	
	public AttachmentMeta createAttachment(byte[] content, String filename) throws AttachmentQuotaException {
		if(checkAndUpateAttachmentQuota()) {
			return createAttachmentWithoutQuotaCheck(content, filename);
		} else {
			logger.debug(context.getExecutionId().toString() + ". Skipping attachment \"" + filename + "\"");
			throw new AttachmentQuotaException("The attachment " + filename + " has been skipped because the test " +
					"generated more than the maximum number of attachments permitted.");
		}
	}

	public AttachmentMeta createAttachmentWithoutQuotaCheck(byte[] content, String filename) {
		ResourceRevisionContainer container;
		try {
			container = resourceManager.createResourceContainer("attachment", filename);
			try {
				BufferedOutputStream bos = new BufferedOutputStream(container.getOutputStream());
				bos.write(content);
				bos.close();
			} catch (IOException ex) {
				logger.error("Unable to write exception.log", ex);
				throw new RuntimeException("Error while ", ex);
			} finally {
				try {
					container.save();
				} catch (IOException e) {
					logger.error("Error while closing resource container", e);
				}
			}
			
			Resource resource = container.getResource();
			AttachmentMeta attachmentMeta = new AttachmentMeta();
			attachmentMeta.setId(resource.getId());
			attachmentMeta.setName(resource.getResourceName());
			return attachmentMeta;
		} catch (IOException e1) {
			throw new RuntimeException("Error while createing resource container", e1);
		}
	}
	
	public void attach(byte[] content, String filename, ReportNode reportNode ) {
		try {
			AttachmentMeta attachment = createAttachment(content, filename);
			reportNode.addAttachment(attachment);
		} catch (AttachmentQuotaException e) {
			logger.debug(context.getExecutionId().toString() + ". Skipping attachment \"" + filename + "\"");
			reportNode.addError("The attachment " + filename + " has been skipped because the test "
					+ "generated more than the maximum number of attachments permitted.");
		}
	}
	

}
