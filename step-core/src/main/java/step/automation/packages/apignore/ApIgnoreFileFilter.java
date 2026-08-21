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
package step.automation.packages.apignore;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The exclusion patterns declared by the {@code .apignore} file at the root of an automation package
 * directory, in a gitignore-like syntax. It decides what belongs to the package: the CLI applies it
 * when it builds the archive to deploy or execute, and the browser applies it when it lists an
 * exploded package, so that the picker offers exactly what would be deployed.
 */
public class ApIgnoreFileFilter {

    public static final String AP_IGNORE_FILE_NAME = ".apignore";

    private final List<PathMatcher> ignoreMatchers = new ArrayList<>();
    private final Path rootDirectory;

    /**
     * @param rootDirectory the root of the automation package, against which the patterns are anchored
     * @return the filter declared by {@code <rootDirectory>/.apignore}, or {@code null} if the package
     * declares none - in which case nothing is filtered out at all
     * @throws IOException if the {@code .apignore} file exists but cannot be read
     */
    public static ApIgnoreFileFilter of(Path rootDirectory) throws IOException {
        Path apIgnoreFile = rootDirectory.resolve(AP_IGNORE_FILE_NAME);
        return Files.isRegularFile(apIgnoreFile) ? new ApIgnoreFileFilter(rootDirectory, apIgnoreFile) : null;
    }

    public ApIgnoreFileFilter(Path rootDirectory, Path gitIgnoreFile) throws IOException {
        this.rootDirectory = rootDirectory;
        loadGitIgnoreFile(gitIgnoreFile);
    }

    private void loadGitIgnoreFile(Path gitIgnoreFile) throws IOException {
        List<String> ignorePatterns = Files.readAllLines(gitIgnoreFile);

        for (String pattern : ignorePatterns) {
            if (pattern.isBlank() || pattern.startsWith("#")) {
                continue; // Skip comments and blank lines
            }

            String globPattern = convertToGlobPattern(pattern);
            ignoreMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + globPattern));
        }
    }

    private String convertToGlobPattern(String gitIgnorePattern) {
        String normalizedPattern = gitIgnorePattern.trim();

        if (!normalizedPattern.startsWith("/") && !normalizedPattern.startsWith("**/")) {
            normalizedPattern = "**/" + normalizedPattern; // Match from any directory level
        }

        // Handle "/**/" for directories, gitignore treat them a 0 to n directories, while glob pattern as at least one
        if (normalizedPattern.contains("/**/")) {
            normalizedPattern = normalizedPattern.replace("/**/", "{,/**}/");
        }

        // Handle trailing slash for directories
        if (normalizedPattern.endsWith("/")) {
            normalizedPattern += "**";
        }

        return normalizedPattern;
    }

    /**
     * @param path a file or directory below the root directory this filter was built for
     * @return whether it belongs to the automation package, i.e. is matched by none of the declared
     * patterns. The {@code .apignore} file itself never belongs to it - wherever it is found, as in the
     * CLI, even though only the one at the root is honoured
     */
    public boolean accept(Path path) {
        Path fileName = path.getFileName();
        if (fileName != null && fileName.toString().equals(AP_IGNORE_FILE_NAME)) {
            return false;
        }

        String relativePathStr = File.separator + rootDirectory.relativize(path).normalize().toString();

        for (PathMatcher matcher : ignoreMatchers) {
            if (matcher.matches(Paths.get(relativePathStr))) {
                return false; // File is ignored
            }
        }
        return true; // File is accepted
    }
}
