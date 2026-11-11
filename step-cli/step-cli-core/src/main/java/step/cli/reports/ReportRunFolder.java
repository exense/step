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
package step.cli.reports;

import step.automation.packages.ApFileNames;
import step.cli.StepCliExecutionException;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The folder of the report directory holding everything a run of the CLI writes, named after the time the run
 * started. It is only created once something is written to it, and is never reused by another run:
 * <pre>
 * reports/20261103-101530-123456/
 *   live.txt
 *   Checkout-tree.txt
 *   Checkout-aggregated.txt
 *   junit/junit.xml
 *   junit/attachments/...
 *   attachments/attachments.tsv
 *   attachments/Checkout/0001-Take_screenshot/page.png
 * </pre>
 * The files and folders of an execution are named after it, typically after its plan, the same way for all of them.
 */
public class ReportRunFolder {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS");
    // Keeps the paths of the files within the limits of Windows, the names of the plans and nodes being as long as one likes
    private static final int MAX_FILE_NAME_LENGTH = 50;
    private static final int MAX_EXTENSION_LENGTH = 10;
    // The characters Windows refuses in a file name, and the white spaces, for the printed paths to be clicked as a whole
    private static final Pattern UNWANTED_FILE_NAME_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}\\s]+");
    private static final Pattern TRAILING_DOTS = Pattern.compile("\\.+$");

    private final File folder;

    // Only used in synchronized methods: the reports and the attachment export ask for names from different threads
    private final Map<String, String> executionFileNames = new HashMap<>();
    private final Set<String> usedExecutionFileNames = new HashSet<>();

    public ReportRunFolder(File reportDir) {
        this(reportDir, LocalDateTime.now());
    }

    public ReportRunFolder(File reportDir, LocalDateTime runStart) {
        Objects.requireNonNull(reportDir);
        Objects.requireNonNull(runStart);
        this.folder = new File(reportDir, TIMESTAMP.format(runStart));
    }

    /**
     * @return the folder of the run, which only exists once something has been written to it
     */
    public File getFolder() {
        return folder;
    }

    /**
     * @return the folder of the run, created along with the report directory if they do not exist yet
     */
    public synchronized File prepare() {
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new StepCliExecutionException("The report folder cannot be created: " + folder.getAbsolutePath());
        }
        return folder;
    }

    /**
     * @param executionName the name of the execution, typically its plan
     * @return the name the files and folders of the execution start with. Executions whose names only differ by the
     * characters a file name cannot hold get a name of their own.
     */
    public synchronized String getExecutionFileName(String executionId, String executionName) {
        Objects.requireNonNull(executionId);
        return executionFileNames.computeIfAbsent(executionId, id -> {
            String baseName = toFileName(executionName != null ? executionName : executionId, "_");
            String name = baseName;
            // Compared regardless of the case, which file systems like the ones of Windows ignore
            for (int i = 2; !usedExecutionFileNames.add(name.toLowerCase()); i++) {
                name = baseName + "-" + i;
            }
            return name;
        });
    }

    /**
     * Turns the name of a plan, a node or an attachment into a file or folder name. The characters Windows refuses and
     * the white spaces become {@code _}, so that the printed paths can be selected or clicked in a terminal as a whole. The
     * name is truncated without losing its extension, and a name Windows reserves, such as {@code CON}, is prefixed.
     *
     * @param fallback the name to use when nothing is left of {@code name}
     */
    public static String toFileName(String name, String fallback) {
        String fileName = name == null ? "" : UNWANTED_FILE_NAME_CHARACTERS.matcher(name.trim()).replaceAll("_");
        // Windows drops the dots ending a file name
        fileName = TRAILING_DOTS.matcher(fileName).replaceAll("");
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            int extensionIndex = fileName.lastIndexOf('.');
            String extension = extensionIndex > 0 && fileName.length() - extensionIndex <= MAX_EXTENSION_LENGTH
                ? fileName.substring(extensionIndex) : "";
            String baseName = fileName.substring(0, MAX_FILE_NAME_LENGTH - extension.length());
            fileName = TRAILING_DOTS.matcher(baseName).replaceAll("") + extension;
        }
        if (fileName.isEmpty()) {
            return fallback;
        }
        return ApFileNames.isReservedOnWindows(fileName) ? "_" + fileName : fileName;
    }
}
