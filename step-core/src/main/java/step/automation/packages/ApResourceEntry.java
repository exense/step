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

/**
 * One entry of an automation package archive, as returned by the {@link ApResourceBrowser} file browser.
 * <p>
 * The field set is deliberately the one of a generic file browser entry rather than the minimum an
 * archive can describe, so that the same payload - and hence the same UI component - can later serve
 * the local file system browser too.
 *
 * @param name              the name of the entry (its last path segment)
 * @param path              the archive-root relative path of the entry, always {@code /} separated
 * @param directory         whether the entry is a folder
 * @param regularFile       whether the entry is a regular file. Always {@code !directory} for an
 *                          archive, but not on a real file system, where an entry can be neither
 *                          (broken symlink, device node)
 * @param hidden            always {@code false}: an archive carries no such attribute, and deriving it
 *                          from the file system in the exploded folder case would make the two archive
 *                          flavours behave differently
 * @param symlink           always {@code false}, for the same reason
 * @param size              the size in bytes, {@code null} for folders and for a zip entry that does
 *                          not declare its size
 * @param resourceReference the ready-to-use reference of this entry, built server side on purpose so
 *                          that clients never have to assemble a reference format themselves. Its form
 *                          depends on the caller: {@code apResource:<apId>:<path>} on the controller,
 *                          the plain relative path in the local IDE mode
 */
public record ApResourceEntry(String name, String path, boolean directory, boolean regularFile,
                              boolean hidden, boolean symlink, Long size, String resourceReference) {
}
