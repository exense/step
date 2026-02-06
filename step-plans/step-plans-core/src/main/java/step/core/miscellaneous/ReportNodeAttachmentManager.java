/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.core.miscellaneous;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentMeta;
import step.attachments.SkippedAttachmentMeta;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.variables.UndefinedVariableException;
import step.core.variables.VariablesManager;
import step.resources.*;


// TODO refactor this class to remove the ExecutionContext dependency 
public class ReportNodeAttachmentManager {

	public static String QUOTA_COUNT_VARNAME = "tec.quota.attachments.count";
	
	public static String QUOTA_VARNAME = "tec.quota.attachments";
	
	private static final Logger logger = LoggerFactory.getLogger(ReportNodeAttachmentManager.class);
	
	private ResourceManager resourceManager;
	
	private final ExecutionContext context;

	public ReportNodeAttachmentManager(ExecutionContext context) {
		super();
		this.context = context;
		ResourceManager contextResourceManager = context.getResourceManager();

		if (contextResourceManager != null) {
			// in case of isolated execution we want to store attachments in permanent resource manager to avoid cleanup
			if (contextResourceManager instanceof LayeredResourceManager) {
				ResourceManager permanentManager = ((LayeredResourceManager) contextResourceManager).getPermanentResourceManager();
				this.resourceManager = permanentManager != null ? permanentManager : contextResourceManager;
			} else {
				this.resourceManager = contextResourceManager;
			}
		}
	}

	// Warning: only use this constructor if you know that you will only use the method createResourceWithoutQuotaCheck,
	// with resource types DIFFERENT from 'attachment' (e.g. 'temp'). Storing attachments, and
	// the other methods, need an ExecutionContext to link attachments to an execution, and to check the quota usage.
	public ReportNodeAttachmentManager(ResourceManager resourceManager) {
		super();
		this.context = null;
		this.resourceManager = resourceManager;
	}

	private boolean checkAndUpateAttachmentQuota() {
		synchronized (context) {
			VariablesManager varManager = context.getVariablesManager();
			
			Integer count;
			try {
				count = varManager.getVariableAsInteger(QUOTA_COUNT_VARNAME) + 1;
				varManager.updateVariable(QUOTA_COUNT_VARNAME, count);
			} catch (UndefinedVariableException e) {
				count = 1;
				varManager.putVariable(context.getReport(), QUOTA_COUNT_VARNAME, count);				
			}

			Integer quota = varManager.getVariableAsInteger(QUOTA_VARNAME, 100);

			if(quota==count) {
				logger.info(context.getExecutionId().toString() + ". Maximum number of attachment (" +quota+") reached. Next attachments will be skipped.");
			}
			
			return quota>=count;
		}		
	}
	
	private static byte[] exceptionToAttachment(Throwable e) {
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		return w.toString().getBytes();
	}

	public void attach(Throwable e, ReportNode node) {
		attach(exceptionToAttachment(e), "exception.log", "text/plain", node);
	}

	public void attach(byte[] content, String filename, String mimeType, ReportNode reportNode ) {
			reportNode.addAttachment(createAttachment(content, filename, mimeType));
	}
	
	public AttachmentMeta createAttachment(byte[] content, String filename, String mimeType) {
		if(checkAndUpateAttachmentQuota()) {
			return createResourceWithoutQuotaCheck(ResourceManager.RESOURCE_TYPE_ATTACHMENT, content, filename, mimeType);
		} else {
			String message = String.format("The attachment %s has been skipped because the execution generated more than" +
					" the maximum number of attachments permitted. This quota can be changed by setting the variable %s with an higher value.", filename, QUOTA_VARNAME);
			if (logger.isDebugEnabled()) {
				logger.debug("Execution {} - {}", context.getExecutionId(), message);
			}
			return new SkippedAttachmentMeta(filename, mimeType, message);
		}
	}

	public AttachmentMeta createResourceWithoutQuotaCheck(String resourceType, byte[] content, String filename, String mimeType) {
		ResourceRevisionContainer container;
		try {
			if (ResourceManager.RESOURCE_TYPE_ATTACHMENT.equals(resourceType) && context == null) {
				throw new IllegalStateException("No context present, unable to create attachment");
			}
			container = resourceManager.createResourceContainer(resourceType, filename, null);
			if (ResourceManager.RESOURCE_TYPE_ATTACHMENT.equals(resourceType)) {
				container.getResource().setExecutionId(context.getExecutionId());
			}
			try {
				BufferedOutputStream bos = new BufferedOutputStream(container.getOutputStream());
				bos.write(content);
				bos.close();
			} catch (IOException ex) {
				logger.error("Unable to write {}", filename, ex);
				throw new RuntimeException("Error while saving attachment " + filename, ex);
			} finally {
				try {
					if (container != null) {
						container.save();
					}
				} catch (IOException | InvalidResourceFormatException e) {
					logger.error("Error while closing resource container", e);
				}
			}
			
			Resource resource = container.getResource();
			AttachmentMeta attachmentMeta = new AttachmentMeta();
			attachmentMeta.setId(resource.getId());
			attachmentMeta.setName(resource.getResourceName());
			attachmentMeta.setMimeType(mimeType);
			return attachmentMeta;
		} catch (IOException e1) {
			throw new RuntimeException("Error while createing resource container", e1);
		}
	}

}
