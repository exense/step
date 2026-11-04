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

import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.streaming.client.upload.StreamingUpload;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * The keyword of the automation package {@link StreamingAttachmentsLocalExecutionTest} builds, run by a Java agent
 */
public class StreamingKeywords extends AbstractKeyword {

    public static final String STREAMED_FILE_NAME = "java-keyword.log";
    public static final String STREAMED_CONTENT = "line 1\nline 2\n";
    public static final String ATTACHED_FILE_NAME = "summary.txt";
    public static final String ATTACHED_CONTENT = "2 lines";

    /**
     * Streams a log while writing it, as a keyword following the progress of a long running process would
     */
    @Keyword(name = "Stream a log")
    public void streamALog() throws Exception {
        Path directory = Files.createTempDirectory("streamed-");
        File log = directory.resolve(STREAMED_FILE_NAME).toFile();
        try {
            Files.writeString(log.toPath(), "");
            StreamingUpload upload = liveReporting.fileUploads.startTextFileUpload(log);
            Files.writeString(log.toPath(), STREAMED_CONTENT);
            upload.complete(Duration.ofSeconds(30));
            // An attachment of the output as well, which the same storage receives
            output.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(ATTACHED_CONTENT.getBytes(StandardCharsets.UTF_8), ATTACHED_FILE_NAME));
        } finally {
            Files.deleteIfExists(log.toPath());
            Files.deleteIfExists(directory);
        }
    }
}
