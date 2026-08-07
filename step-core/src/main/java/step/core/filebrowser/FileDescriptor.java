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

/**
 * One entry of a browsable namespace, whatever that namespace is: the local file system of the
 * controller, or the content of an automation package.
 * <p>
 * The shape is shared on purpose, so that a single UI component can list any of them. What varies is
 * the meaning of {@link #path()} and {@link #resourceReference()}, never the field set.
 *
 * @param name              the display name of the entry, its last path segment
 * @param path              the identity of the entry within its namespace: an absolute path on a file
 *                          system, an archive-root relative path in an automation package. Always
 *                          {@code /} separated in the latter case
 * @param directory         whether the entry is a directory
 * @param regularFile       whether the entry is a regular file. Not simply {@code !directory}: on a
 *                          real file system an entry can be neither, e.g. a broken symlink or a
 *                          device node
 * @param hidden            whether the entry is hidden. Always {@code false} for a namespace that has
 *                          no such notion
 * @param symlink           whether the entry is a symbolic link. Always {@code false} for a namespace
 *                          that has no such notion
 * @param size              the size in bytes, {@code null} when it does not apply (a directory) or is
 *                          not known (a zip entry that does not declare it)
 * @param resourceReference the value to be stored by the client when this entry is picked, built
 *                          server side on purpose so that clients never have to assemble a reference
 *                          format themselves. Depending on the namespace: an absolute path, an
 *                          {@code apResource:<apId>:<path>} reference, or a plain relative path
 */
public record FileDescriptor(String name, String path, boolean directory, boolean regularFile,
                             boolean hidden, boolean symlink, Long size, String resourceReference) {
}
