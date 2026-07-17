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
package step.artefacts.handlers;

import step.artefacts.Export;
import step.attachments.AttachmentMeta;
import step.attachments.SkippedAttachmentMeta;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.resources.AttachmentStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

public class ExportHandler extends ArtefactHandler<Export, ReportNode> {

    @Override
    protected void createReportSkeleton_(ReportNode parentNode, Export testArtefact) {
        export(parentNode, testArtefact);
    }

    @Override
    protected void execute_(ReportNode node, Export testArtefact) {
        node.setStatus(ReportNodeStatus.PASSED);
        export(node, testArtefact);
    }

    private void export(ReportNode node, Export testArtefact) {

        Pattern filter = null;
        if (testArtefact.getFilter() != null) {
            String filterExpr = testArtefact.getFilter().get();
            if (filterExpr != null && filterExpr.trim().length() > 0) {
                filter = Pattern.compile(filterExpr);
            }
        }

        String filename = testArtefact.getFile().get();
        if (filename != null) {
            File file = new File(filename);
            if (!file.exists()) {
                file.mkdirs();
            }

            if (testArtefact.getValue() != null) {
                Object value = testArtefact.getValue().get();
                if (value != null) {
                    if (value instanceof List<?>) {
                        if (file.isDirectory()) {
                            List<?> list = (List<?>) value;
                            for (Object object : list) {
                                if (object instanceof AttachmentMeta attachmentMeta) {
                                    if (attachmentMeta instanceof SkippedAttachmentMeta) {
                                        continue;
                                    }
                                    // Export only if the file name matches the defined filter
                                    String fileName = attachmentMeta.getName();
                                    if (filter == null || filter.matcher(fileName).matches()) {
                                        AttachmentStorage storage = context.getAttachmentStorage();
                                        if (storage == null) {
                                            throw new RuntimeException("Attachment storage is null");
                                        }
                                        String filenamePrefix = testArtefact.getPrefix() != null ? testArtefact.getPrefix().get() : "";
                                        File target = new File(file + "/" + filenamePrefix + fileName);
                                        try (InputStream in = storage.getAttachmentStream(attachmentMeta.getId().toString());
                                             OutputStream out = new FileOutputStream(target)) {
                                            in.transferTo(out);
                                        } catch (Exception e) {
                                            throw new RuntimeException("Error while saving " + fileName + " to " + target.getAbsolutePath());
                                        }
                                    }
                                }
                            }
                        } else {
                            fail(node, "The folder " + file.getPath() + " is not a directory. Please set the argument 'File' to the export's output directory.");
                        }
                    } else {
                        fail(node, "The object defined by the value expression '" + testArtefact.getValue().getExpression() + "' is not a list. The Export control currently only supports the export of attachment lists.");
                    }
                } else {
                    fail(node, "The object defined by the value expression '" + testArtefact.getValue().getExpression() + "' is null. Please change the value expression to point to the object to be exported.");
                }
            } else {
                fail(node, "The argument 'Value' is null or hasn't been set. Please set the argument 'Value' to the object that should be exported.");
            }
        } else {
            fail(node, "The argument 'File' is null or hasn't been set. Please set the argument 'File' to the export's output directory.");
        }
    }

    @Override
    public ReportNode createReportNode_(ReportNode parentNode, Export testArtefact) {
        return new ReportNode();
    }
}
