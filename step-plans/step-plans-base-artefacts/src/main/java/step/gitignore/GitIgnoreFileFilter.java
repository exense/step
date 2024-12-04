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
package step.gitignore;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

public class GitIgnoreFileFilter {

    private final List<PathMatcher> ignoreMatchers = new ArrayList<>();
    private final Path rootDirectory;

    public GitIgnoreFileFilter(Path rootDirectory, Path gitIgnoreFile) throws IOException {
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

        if (normalizedPattern.endsWith("/")) {
            normalizedPattern += "**"; // Match directories recursively
        }

        if (!normalizedPattern.startsWith("/")) {
            normalizedPattern = "**/" + normalizedPattern; // Match from any directory level
        }

        return normalizedPattern;
    }

    public boolean accept(Path path) {
        Path relativePath = rootDirectory.relativize(path);

        for (PathMatcher matcher : ignoreMatchers) {
            if (matcher.matches(relativePath)) {
                return false; // File is ignored
            }
        }
        return true; // File is accepted
    }

}