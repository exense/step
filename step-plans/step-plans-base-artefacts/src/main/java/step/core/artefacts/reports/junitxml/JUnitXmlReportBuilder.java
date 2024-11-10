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
package step.core.artefacts.reports.junitxml;

import ch.exense.commons.io.FileHelper;
import com.google.common.io.Files;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.AttachmentMeta;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.execution.ExecutionEngineContext;
import step.reporting.AttachmentsConfig;
import step.reporting.JUnit4ReportWriter;
import step.reporting.JUnitReport;
import step.reporting.ReportMetadata;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContent;

import java.io.*;
import java.util.List;

public class JUnitXmlReportBuilder {

    private static final Logger log = LoggerFactory.getLogger(JUnitXmlReportBuilder.class);

    public static final String DEFAULT_ATTACHMETS_SUBFOLDER = "attachments";

    private final ReportNodeAccessor reportNodeAccessor;
    private final ResourceManager attachmentsResourceManager;

    public JUnitXmlReportBuilder(ExecutionEngineContext executionEngineContext) {
        this.reportNodeAccessor = executionEngineContext.getReportNodeAccessor();
        this.attachmentsResourceManager = executionEngineContext.getResourceManager();
    }

    public JUnitReport buildJUnitXmlReport(List<String> executionIds) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(baos)) {
            ReportMetadata reportMetadata = buildJUnitXmlReport(new JUnit4ReportWriter(), executionIds, writer);
            return new JUnitReport(reportMetadata.getFileName(), baos.toByteArray());
        }
    }

    public JUnitReport buildJunitZipReport(List<String> executionIds, Boolean includeAttachments, String attachmentsSubfolder) throws IOException {
        AttachmentsConfig attachmentsConfig = null;
        attachmentsSubfolder = attachmentsSubfolder == null || attachmentsSubfolder.isEmpty() ? DEFAULT_ATTACHMETS_SUBFOLDER : attachmentsSubfolder;
        if (includeAttachments != null && includeAttachments) {
            attachmentsConfig = new AttachmentsConfig.Builder()
                    .setAttachmentSubfolder(attachmentsSubfolder)
                    .setAttachmentResourceManager(attachmentsResourceManager)
                    .createAttachmentsConfig();
        }

        try (ByteArrayOutputStream mainReportOutput = new ByteArrayOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(mainReportOutput)) {
            ReportMetadata junitXmlReport = buildJUnitXmlReport(new JUnit4ReportWriter(attachmentsConfig), executionIds, writer);

            // here we prepare the temporary directory with xml report and attachments znd zip the whole directory
            File reportDir = FileHelper.createTempFolder();
            String reportNameWithoutExtension = Files.getNameWithoutExtension(junitXmlReport.getFileName());
            try {
                File xmlReportFile = new File(reportDir, junitXmlReport.getFileName());
                Files.write(mainReportOutput.toByteArray(), xmlReportFile);

                if (junitXmlReport.getAttachmentMetas() != null && !junitXmlReport.getAttachmentMetas().isEmpty()) {
                    File attachmentsDir = new File(reportDir, attachmentsSubfolder);
                    boolean mkdir = attachmentsDir.mkdir();
                    if (!mkdir) {
                        throw new IOException("Unable to create subdirectory for attachments: " + attachmentsDir.getAbsolutePath());
                    }

                    for (AttachmentMeta attachmentMeta : junitXmlReport.getAttachmentMetas()) {
                        ObjectId attachmentId = attachmentMeta.getId();
                        ResourceRevisionContent attachmentResource = attachmentsResourceManager.getResourceContent(attachmentId.toString());
                        File attachmentOutFile = new File(attachmentsDir, attachmentResource.getResourceName());
                        try (FileOutputStream attachmentOutStream = new FileOutputStream(attachmentOutFile)) {
                            FileHelper.copy(attachmentResource.getResourceStream(), attachmentOutStream);
                        }
                    }
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    FileHelper.zip(reportDir, baos);
                    return new JUnitReport(reportNameWithoutExtension + ".zip", baos.toByteArray());
                }
            } finally {
                boolean deleted = FileHelper.deleteFolder(reportDir);
                if (!deleted) {
                    log.warn("Temp dir cannot be deleted: " + reportDir.getAbsolutePath());
                }
            }
        }

    }

    public ReportMetadata buildJUnitXmlReport(JUnit4ReportWriter reportWriter, List<String> executionIds, OutputStreamWriter writer) throws IOException {
        ReportMetadata reportMetadata;
        if (executionIds.size() > 1) {
            reportMetadata = reportWriter.writeMultiReport(reportNodeAccessor, executionIds, writer);
        } else {
            reportMetadata = reportWriter.writeReport(reportNodeAccessor, executionIds.get(0), writer);
        }
        return reportMetadata;
    }

}
