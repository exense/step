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
 *
 * @param name      the name of the entry (its last path segment)
 * @param path      the archive-root relative path of the entry, always {@code /} separated
 * @param directory whether the entry is a folder
 * @param size      the size in bytes, {@code null} for folders
 * @param reference the ready-to-use {@code apResource:<apId>:<path>} reference of this entry. Built
 *                  server side on purpose, so that clients never have to assemble the reference
 *                  format themselves.
 */
public record ApResourceEntry(String name, String path, boolean directory, Long size, String reference) {
}
