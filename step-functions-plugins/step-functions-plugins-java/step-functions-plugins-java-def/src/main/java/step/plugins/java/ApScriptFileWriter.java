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
package step.plugins.java;

import ch.exense.commons.app.Configuration;
import step.attachments.FileResolver;
import step.automation.packages.LocalApResourceWriter;
import step.core.accessors.AbstractOrganizableObject;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Creates the script file of a keyword <b>inside the automation package being edited</b>, and references
 * it as {@code apResource:local:<relativePath>} - which the descriptor holds as the plain relative path.
 * <p>
 * This is the editor counterpart of what {@link AbstractScriptFunctionType} does on a Step server, where
 * a new script becomes a Step {@code Resource}.
 * <p>
 * The file it writes is <b>the user's source</b>, hence the rules {@link LocalApResourceWriter} enforces:
 * an existing file is never overwritten, and a generated name is derived from the keyword name rather
 * than from a uuid.
 *
 * @see step.automation.packages.ApResourceProvider#getEditableRoot()
 */
public class ApScriptFileWriter {

    /**
     * Where the script of a keyword created from the editor is placed, relative to the package root.
     * Overridable with the {@value #SCRIPT_DIRECTORY_PROPERTY} property.
     */
    public static final String DEFAULT_SCRIPT_DIRECTORY = "keywords";

    public static final String SCRIPT_DIRECTORY_PROPERTY = "keywords.script.ap.dir";

    private final Path automationPackageRoot;
    private final String scriptDirectory;

    /**
     * @param automationPackageRoot the root directory of the automation package open in the editor, as
     *                              returned by {@code ApResourceProvider.getEditableRoot()}
     */
    public ApScriptFileWriter(Path automationPackageRoot, Configuration configuration) {
        this.automationPackageRoot = Objects.requireNonNull(automationPackageRoot, "automationPackageRoot must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        this.scriptDirectory = configuration.getProperty(SCRIPT_DIRECTORY_PROPERTY, DEFAULT_SCRIPT_DIRECTORY);
    }

    /**
     * Creates the script of a newly created keyword and sets its {@code scriptFile} to the reference of
     * the created file.
     * <p>
     * A path the user already provided is kept as it is and only materialised if missing, so that a
     * keyword created against an existing script does not get a second file, nor lose its content. A
     * keyword created against something the editor does not own - a Step resource, another automation
     * package - is left alone entirely: the script exists already, there is nothing to set up.
     *
     * @param fileExtension  the extension of the created file, without the dot
     * @param templateStream the initial content, {@code null} for an empty script
     * @return the created or referenced file, or {@code null} if there was nothing to create
     */
    public File create(GeneralScriptFunction function, String fileExtension, InputStream templateStream) throws SetupFunctionException {
        String scriptFilename = function.getScriptFile().get();
        if (FileResolver.isResource(scriptFilename)
            || (FileResolver.isApResource(scriptFilename) && !FileResolver.isLocalApResource(scriptFilename))) {
            return null;
        }
        try {
            String relativePath;
            if (scriptFilename == null || scriptFilename.isBlank()) {
                relativePath = LocalApResourceWriter.createFile(automationPackageRoot, scriptDirectory,
                    function.getAttributes().get(AbstractOrganizableObject.NAME), fileExtension, templateStream);
            } else {
                relativePath = FileResolver.normalizeApRelativePath(FileResolver.isLocalApResource(scriptFilename)
                    ? FileResolver.extractApRelativePath(scriptFilename) : scriptFilename);
                LocalApResourceWriter.createFileIfMissing(automationPackageRoot, relativePath, templateStream);
            }
            function.getScriptFile().setValue(FileResolver.createPathForLocalApResource(relativePath));
            return automationPackageRoot.resolve(relativePath).toFile();
        } catch (IOException | IllegalArgumentException e) {
            throw new SetupFunctionException("Unable to create the script of the keyword '"
                + function.getAttributes().get(AbstractOrganizableObject.NAME) + "' in the automation package "
                + automationPackageRoot, e);
        }
    }

    /**
     * Copies the script of a cloned keyword <b>next to its source</b> so that the layout the user chose is
     * preserved, and under the name of the copy.
     * <p>
     * Unlike the server path this one does not swallow its failures: falling back to the source reference
     * would leave the two keywords writing the same script file, which is neither visible in the editor
     * nor something the user would expect from a copy.
     *
     * @param sourceReference the {@code scriptFile} of the keyword being cloned
     * @param fileResolver    resolves {@code sourceReference} to the file to copy
     * @return the {@code apResource:local:} reference of the created copy, or an empty string if the
     * cloned keyword had no script yet
     */
    public String copy(GeneralScriptFunction copy, String fileExtension, String sourceReference,
                       FileResolver fileResolver) throws FunctionTypeException {
        if (sourceReference == null || sourceReference.isBlank()) {
            // a keyword with no script yet: the copy has none either, rather than a file copied from nowhere
            return "";
        }
        String directory = scriptDirectory;
        if (FileResolver.isLocalApResource(sourceReference)) {
            // a source outside the package - a Step resource, another package - keeps the default
            // directory: there is no layout of ours to preserve
            String sourcePath = FileResolver.normalizeApRelativePath(FileResolver.extractApRelativePath(sourceReference));
            int lastSeparator = sourcePath.lastIndexOf('/');
            directory = lastSeparator < 0 ? null : sourcePath.substring(0, lastSeparator);
        }
        try (InputStream sourceStream = new FileInputStream(fileResolver.resolve(sourceReference))) {
            String relativePath = LocalApResourceWriter.createFile(automationPackageRoot, directory,
                copy.getAttributes().get(AbstractOrganizableObject.NAME), fileExtension, sourceStream);
            return FileResolver.createPathForLocalApResource(relativePath);
        } catch (IOException | RuntimeException e) {
            throw new FunctionTypeException("Unable to copy the script '" + sourceReference + "' for the keyword '"
                + copy.getAttributes().get(AbstractOrganizableObject.NAME) + "'", e);
        }
    }
}
