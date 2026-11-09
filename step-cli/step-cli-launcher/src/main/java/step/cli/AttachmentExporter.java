/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
package step.cli;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.AttachmentMeta;
import step.attachments.SkippedAttachmentMeta;
import step.cli.reports.ReportRunFolder;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.resources.AttachmentStorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes the attachments of the report nodes of local executions to disk, before the execution engine deletes them
 * along with its temporary storage. They are written to the attachments folder of the folder of the run, created the
 * first time a node has attachments:
 * <pre>
 * reports/20261102-143012-123456/attachments/
 *   attachments.tsv
 *   Online_shop/
 *     0001-Place_order/exception.log
 *     0002-Take_screenshot/screenshot.png
 * </pre>
 * Each plan gets a folder named like its report files, and each node a folder numbered in the order the nodes end,
 * holding its attachments under their own names. The index {@code attachments.tsv} tells which node each of them
 * belongs to: the plan, the path of the node from the root of the plan, as printed by the live report, its status and
 * its id.
 */
public class AttachmentExporter {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentExporter.class);

    public static final String FOLDER_NAME = "attachments";
    public static final String INDEX_FILE_NAME = "attachments.tsv";

    private static final String INDEX_HEADER = String.join("\t", "Plan", "Path", "Status", "Report node", "Attachment", "File", "Skipped reason");

    private final ReportRunFolder runFolder;
    private final Path directory;

    // The fields below are only used in synchronized methods: the nodes of parallel steps end in different threads,
    // which export their attachments at the same time
    private final Map<String, Path> planDirectories = new HashMap<>();
    private final Map<ObjectId, List<ExportedAttachment>> exportedAttachments = new HashMap<>();
    private int nodeCount;
    private int fileCount;

    public AttachmentExporter(ReportRunFolder runFolder) {
        this.runFolder = Objects.requireNonNull(runFolder);
        this.directory = runFolder.getFolder().toPath().resolve(FOLDER_NAME);
    }

    /**
     * @return the folder the attachments are written to, which only exists once one of them has been written
     */
    public Path getDirectory() {
        return directory;
    }

    public synchronized int getFileCount() {
        return fileCount;
    }

    /**
     * Writes the attachments of a node which has ended
     *
     * @param nodePath the path of the node from the root of its plan
     * @return what became of each attachment of the node, an empty list if it has none
     */
    public synchronized List<ExportedAttachment> export(ExecutionContext context, ReportNode node, String nodePath) {
        List<AttachmentMeta> attachments = node.getAttachments();
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        List<ExportedAttachment> exported = new ArrayList<>();
        try {
            Path planDirectory = getPlanDirectory(context);
            Path nodeDirectory = Files.createDirectories(planDirectory.resolve(
                String.format("%04d-%s", ++nodeCount, ReportRunFolder.toFileName(node.getName(), "_"))));
            for (AttachmentMeta attachment : attachments) {
                ExportedAttachment exportedAttachment = export(context.getAttachmentStorage(), attachment, nodeDirectory);
                exported.add(exportedAttachment);
                appendToIndex(context, node, nodePath, exportedAttachment);
            }
        } catch (IOException e) {
            logger.error("Unable to write the attachments of the report node {} to {}", nodePath, directory, e);
            // The attachments which could not be written are reported as skipped, for none to go unnoticed
            Set<AttachmentMeta> handled = exported.stream().map(ExportedAttachment::attachment).collect(Collectors.toSet());
            attachments.stream().filter(a -> !handled.contains(a))
                .forEach(a -> exported.add(ExportedAttachment.skipped(a, e.getMessage())));
        }
        exportedAttachments.put(node.getId(), exported);
        return exported;
    }

    /**
     * @return the lines describing the attachments of a node that {@link #export} wrote, for them to be printed with
     * the node
     */
    public synchronized List<String> describeAttachments(ReportNode node) {
        return exportedAttachments.getOrDefault(node.getId(), List.of()).stream()
            .map(ExportedAttachment::describe)
            .collect(Collectors.toList());
    }

    private ExportedAttachment export(AttachmentStorage attachmentStorage, AttachmentMeta attachment, Path nodeDirectory) {
        if (attachment instanceof SkippedAttachmentMeta skipped) {
            return ExportedAttachment.skipped(attachment, skipped.getReason());
        }
        if (attachmentStorage == null || attachment.getId() == null) {
            return ExportedAttachment.skipped(attachment, "not stored by the execution");
        }
        Path file = uniqueFile(nodeDirectory, ReportRunFolder.toFileName(attachment.getName(), "attachment"));
        try (InputStream content = attachmentStorage.getAttachmentStream(attachment.getId().toHexString())) {
            Files.copy(content, file);
            fileCount++;
            return new ExportedAttachment(attachment, file, null);
        } catch (Exception e) {
            logger.error("Unable to write the attachment {} to {}", attachment.getName(), file, e);
            return ExportedAttachment.skipped(attachment, e.getMessage());
        }
    }

    private Path getPlanDirectory(ExecutionContext context) throws IOException {
        Path planDirectory = planDirectories.get(context.getExecutionId());
        if (planDirectory == null) {
            // lazy initialization of the parent directory in case it doesn't exist yet
            if (!Files.isDirectory(directory)) {
                Files.createDirectories(directory);
                Files.writeString(directory.resolve(INDEX_FILE_NAME), INDEX_HEADER + "\n", StandardCharsets.UTF_8);
            }
            planDirectory = directory.resolve(runFolder.getExecutionFileName(context.getExecutionId(), ReportNodePaths.planNameOf(context)));
            planDirectories.put(context.getExecutionId(), planDirectory);
        }
        return planDirectory;
    }

    private void appendToIndex(ExecutionContext context, ReportNode node, String nodePath, ExportedAttachment exported) throws IOException {
        String line = Stream.of(ReportNodePaths.planNameOf(context), nodePath, String.valueOf(node.getStatus()), node.getId().toHexString(),
                exported.attachment().getName(),
                exported.file() != null ? directory.relativize(exported.file()).toString().replace('\\', '/') : "",
                exported.skippedReason() != null ? exported.skippedReason() : "")
            .map(AttachmentExporter::toIndexField)
            .collect(Collectors.joining("\t"));
        Files.writeString(directory.resolve(INDEX_FILE_NAME), line + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    // Replace the characters that would break the TSV format: the tabs separate the columns, the line breaks the rows.
    private static String toIndexField(String value) {
        return value == null ? "" : value.replaceAll("[\\t\\r\\n]+", " ");
    }

    private static Path uniqueFile(Path directory, String fileName) {
        Path file = directory.resolve(fileName);
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        String extension = extensionIndex > 0 ? fileName.substring(extensionIndex) : "";
        for (int i = 2; Files.exists(file); i++) {
            file = directory.resolve(baseName + "-" + i + extension);
        }
        return file;
    }

    /**
     * What became of an attachment: the file it was written to, or the reason it was not
     */
    public record ExportedAttachment(AttachmentMeta attachment, Path file, String skippedReason) {

        private static ExportedAttachment skipped(AttachmentMeta attachment, String reason) {
            return new ExportedAttachment(attachment, null, reason != null ? reason : "unknown reason");
        }

        /**
         * @return the line describing the attachment, the file being given as an absolute file URI, which terminals
         * and IDE consoles turn into a link
         */
        public String describe() {
            return file != null ? "attachment: " + file.toAbsolutePath().toUri()
                : "attachment skipped: " + attachment.getName() + " (" + skippedReason + ")";
        }
    }
}
