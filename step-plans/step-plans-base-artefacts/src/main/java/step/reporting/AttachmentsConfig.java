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
package step.reporting;

import step.resources.ResourceManager;

public class AttachmentsConfig {

    private String attachmentSubfolder;
    private ResourceManager attachmentResourceManager;

    private AttachmentsConfig(String attachmentSubfolder, ResourceManager attachmentResourceManager) {
        this.attachmentSubfolder = attachmentSubfolder;
        this.attachmentResourceManager = attachmentResourceManager;
    }

    public String getAttachmentSubfolder() {
        return attachmentSubfolder;
    }

    public ResourceManager getAttachmentResourceManager() {
        return attachmentResourceManager;
    }

    public static class Builder {
        private String attachmentSubfolder;
        private ResourceManager attachmentResourceManager;

        public Builder setAttachmentSubfolder(String attachmentSubfolder) {
            this.attachmentSubfolder = attachmentSubfolder;
            return this;
        }

        public Builder setAttachmentResourceManager(ResourceManager attachmentResourceManager) {
            this.attachmentResourceManager = attachmentResourceManager;
            return this;
        }

        public AttachmentsConfig createAttachmentsConfig() {
            return new AttachmentsConfig(attachmentSubfolder, attachmentResourceManager);
        }
    }
}
