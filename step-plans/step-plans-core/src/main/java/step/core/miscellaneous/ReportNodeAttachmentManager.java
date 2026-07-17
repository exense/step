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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.AttachmentMeta;
import step.attachments.SkippedAttachmentMeta;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.resources.AttachmentStorage;
import step.resources.InvalidResourceFormatException;
import step.resources.LayeredResourceManager;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


// TODO refactor this class to remove the ExecutionContext dependency
public class ReportNodeAttachmentManager {

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

    private static byte[] exceptionToAttachment(Throwable e) {
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        return w.toString().getBytes();
    }

    public void attach(Throwable e, ReportNode node) {
        attach(exceptionToAttachment(e), "exception.log", "text/plain", node);
    }

    public void attach(byte[] content, String filename, String mimeType, ReportNode reportNode) {
        reportNode.addAttachment(createAttachment(content, filename, mimeType));
    }

    public AttachmentMeta createAttachment(byte[] content, String filename, String mimeType) {
        AttachmentStorage attachmentStorage = context.getAttachmentStorage();
        if (attachmentStorage == null) {
            logger.error("{}: Unable to create attachment {} because attachmentStorage is not set", context, filename);
            return new SkippedAttachmentMeta(filename, mimeType, "attachmentStorage not defined");
        }
        return attachmentStorage.saveAttachment(context, content, filename, mimeType);
    }

    // By now, this method is only used for creating resources of type RESOURCE_TYPE_TEMP.
    public AttachmentMeta createResourceWithoutQuotaCheck(String resourceType, byte[] content, String filename, String mimeType) {
        if (!ResourceManager.RESOURCE_TYPE_TEMP.equals(resourceType)) {
            throw new IllegalArgumentException("The only supported resource type for this operation is: " + ResourceManager.RESOURCE_TYPE_TEMP);
        }
        ResourceRevisionContainer container;
        try {
            container = resourceManager.createResourceContainer(resourceType, filename, null);
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
            throw new RuntimeException("Error while creating resource container", e1);
        }
    }

}
