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
package step.cli.apignore;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApIgnoreFileFilter {

    private final List<PathMatcher> ignoreMatchers = new ArrayList<>();
    private final Path rootDirectory;

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

    public boolean accept(Path path) {
        String relativePathStr = File.separator + rootDirectory.relativize(path).normalize().toString();

        for (PathMatcher matcher : ignoreMatchers) {
            if (matcher.matches(Paths.get(relativePathStr))) {
                return false; // File is ignored
            }
        }
        return true; // File is accepted
    }
}