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

import ch.exense.commons.app.Configuration;
import step.resources.ResourceManager;

public class Junit4ReportConfig {

    private boolean addAttachments;
    private final boolean addLinksToStepFrontend;
    private String attachmentsSubfolder;
    private String attachmentsRootFolder;
    private ResourceManager attachmentResourceManager;
    private Configuration serverConfiguration;

    private Junit4ReportConfig(String attachmentsSubfolder, String attachmentsRootFolder,
                               ResourceManager attachmentResourceManager,
                               boolean addAttachments, boolean addLinksToStepFrontend,
                               Configuration serverConfiguration) {
        this.attachmentsSubfolder = attachmentsSubfolder;
        this.attachmentsRootFolder = attachmentsRootFolder;
        this.attachmentResourceManager = attachmentResourceManager;
        this.addAttachments = addAttachments;
        this.addLinksToStepFrontend = addLinksToStepFrontend;
        this.serverConfiguration = serverConfiguration;
    }

    public String getAttachmentsSubfolder() {
        return attachmentsSubfolder;
    }

    public String getAttachmentsRootFolder() {
        return attachmentsRootFolder;
    }

    public ResourceManager getAttachmentResourceManager() {
        return attachmentResourceManager;
    }

    public boolean isAddAttachments() {
        return addAttachments;
    }

    public boolean isAddLinksToStepFrontend() {
        return addLinksToStepFrontend;
    }

    public Configuration getServerConfiguration() {
        return serverConfiguration;
    }

    public static class Builder {
        private boolean addAttachments = false;
        private boolean addLinksToStepFrontend = true;
        private String attachmentSubfolder;
        private String attachmentRootFolder;
        private ResourceManager attachmentResourceManager;
        private Configuration serverConfiguration;

        public Builder setAttachmentSubfolder(String attachmentSubfolder) {
            this.attachmentSubfolder = attachmentSubfolder;
            return this;
        }

        public Builder setAttachmentRootFolder(String attachmentRootFolder) {
            this.attachmentRootFolder = attachmentRootFolder;
            return this;
        }

        public Builder setAttachmentResourceManager(ResourceManager attachmentResourceManager) {
            this.attachmentResourceManager = attachmentResourceManager;
            return this;
        }

        public Builder setAddAttachments(boolean addAttachments) {
            this.addAttachments = addAttachments;
            return this;
        }

        public Builder setAddLinksToStepFrontend(boolean addLinksToStepFrontend) {
            this.addLinksToStepFrontend = addLinksToStepFrontend;
            return this;
        }

        public Builder setServerConfiguration(Configuration serverConfiguration) {
            this.serverConfiguration = serverConfiguration;
            return this;
        }

        public Junit4ReportConfig createConfig() {
            return new Junit4ReportConfig(
                    attachmentSubfolder, attachmentRootFolder, attachmentResourceManager,
                    addAttachments, addLinksToStepFrontend, serverConfiguration
            );
        }

    }
}
