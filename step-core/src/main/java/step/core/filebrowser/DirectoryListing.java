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
package step.core.filebrowser;

import java.util.List;

/**
 * The content of a single directory of a browsable namespace, the response of every browse service.
 *
 * @param path              the directory that has actually been listed, which may differ from the one
 *                          that was requested - a browser is normally opened on the file the edited
 *                          field currently holds, and lists its parent
 * @param parentPath        the path of the parent directory, or {@code null} when the top of the
 *                          namespace is listed
 * @param resourceReference the reference of the listed directory itself, or {@code null} when it has
 *                          none. Needed to address a directory that is empty and therefore has no
 *                          entry to pick it from
 * @param entries           the direct children of the directory, directories first and then files,
 *                          each group sorted by name - see
 *                          {@link FileDescriptors#byDirectoryThenName()}
 */
public record DirectoryListing(String path, String parentPath, String resourceReference,
                               List<FileDescriptor> entries) {
}
