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

import step.attachments.FileResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Creates files inside the automation package open in the editor - the write counterpart of
 * {@link LocalApResourceProvider}, used when the editor has to produce a file rather than resolve one:
 * the script of a newly created keyword, the copy made when a keyword is cloned.
 * <p>
 * What it writes is <b>the user's source</b>, in the directory they opened and most likely under version
 * control. Hence the two rules that shape the whole class: an existing file is never overwritten (a
 * colliding name gets a {@code _2}, {@code _3}, ... suffix), and the generated name is derived from the
 * entity name rather than from a UUID, the way {@code AbstractScriptFunctionType.getScriptFilename} does
 * for a Step resource - a resource is flat and anonymous, a file in a repository is read by people.
 *
 * @see ApResourceProvider#getEditableRoot()
 */
public final class LocalApResourceWriter {

    /**
     * Enough to make a collision loop pointless rather than to bound anything real: names collide
     * because two keywords share a name, not thousands.
     */
    private static final int MAX_COLLISION_SUFFIX = 1000;

    private LocalApResourceWriter() {
    }

    /**
     * Creates a new file under {@code apRoot}, named after {@code baseName}, without ever overwriting an
     * existing one.
     *
     * @param apRoot    the root directory of the automation package, as returned by
     *                  {@link ApResourceProvider#getEditableRoot()}
     * @param directory the directory to create the file in, relative to the package root; {@code null}
     *                  or blank for the root itself
     * @param baseName  the name to derive the file name from, typically the name of the entity the file
     *                  belongs to. Sanitised, see {@link ApFileNames#sanitize(String)}
     * @param extension the extension, without the dot; {@code null} or blank for none
     * @param content   the initial content, {@code null} for an empty file
     * @return the path of the created file, relative to the package root and normalised - ready to be
     * turned into a reference with {@link FileResolver#createPathForLocalApResource(String)}
     * @throws IOException              if the file cannot be created, or if no free name was found
     * @throws IllegalArgumentException if {@code directory} escapes the package root, or if
     *                                  {@code baseName} yields no usable file name
     */
    public static String createFile(Path apRoot, String directory, String baseName, String extension,
                                    InputStream content) throws IOException {
        Objects.requireNonNull(apRoot, "apRoot must not be null");
        String directoryPart = (directory == null || directory.isBlank())
            ? "" : FileResolver.normalizeApRelativePath(directory) + "/";
        String name = ApFileNames.sanitize(baseName);
        String suffix = (extension == null || extension.isBlank()) ? "" : "." + extension;

        for (int i = 1; i <= MAX_COLLISION_SUFFIX; i++) {
            String candidate = FileResolver.normalizeApRelativePath(
                directoryPart + name + (i == 1 ? "" : "_" + i) + suffix);
            Path target = apRoot.resolve(candidate);
            Files.createDirectories(target.getParent());
            try {
                // createFile rather than exists() + write: the check and the creation are one step, so
                // two keywords created at once cannot end up sharing a file
                Files.createFile(target);
            } catch (FileAlreadyExistsException e) {
                continue;
            }
            if (content != null) {
                Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return candidate;
        }
        throw new IOException("Unable to create a file named '" + name + suffix + "' in '" + directoryPart
            + "' of the automation package " + apRoot + ": " + MAX_COLLISION_SUFFIX + " names are taken");
    }

    /**
     * Creates the file {@code relativePath} of the package with the given content <b>if it does not
     * exist</b>, and returns it either way. Used when the path was chosen by the user rather than
     * generated: the editor makes sure the file they referenced exists, and leaves its content alone if
     * it does.
     *
     * @throws IllegalArgumentException if {@code relativePath} escapes the package root
     */
    public static File createFileIfMissing(Path apRoot, String relativePath, InputStream content) throws IOException {
        Objects.requireNonNull(apRoot, "apRoot must not be null");
        Path target = apRoot.resolve(FileResolver.normalizeApRelativePath(relativePath));
        if (!Files.exists(target)) {
            Files.createDirectories(target.getParent());
            if (content != null) {
                Files.copy(content, target);
            } else {
                Files.createFile(target);
            }
        }
        return target.toFile();
    }

}
