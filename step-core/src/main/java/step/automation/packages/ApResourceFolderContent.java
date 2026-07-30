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
package step.automation.packages;

import java.util.List;

/**
 * The content of a single folder of an automation package archive.
 *
 * @param apId       the automation package the listed folder belongs to
 * @param path       the archive-root relative path of the folder that has actually been listed;
 *                   empty for the archive root. It may differ from the requested path, see
 *                   {@link ApResourceBrowser#browse(String, java.io.File, String)}
 * @param parentPath the path of the parent folder, or {@code null} when the archive root is listed
 * @param entries    the direct children of the folder: directories first, then files, each group
 *                   sorted alphabetically
 */
public record ApResourceFolderContent(String apId, String path, String parentPath, List<ApResourceEntry> entries) {
}
