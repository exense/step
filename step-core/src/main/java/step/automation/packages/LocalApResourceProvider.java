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
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resolves the {@code apResource:local:<relativePath>} references of the automation package open in
 * the editor. The package is an exploded directory on disk, so the entry <b>is</b> the file: it is
 * returned as it lies, with no materialisation and no cache.
 * <p>
 * That is not an optimisation but a requirement. The editor reads <i>and writes</i> through the
 * resolved file — {@code ScriptEditorServices} saves a keyword script straight into it — so handing
 * out a copy, as the deployed {@code AutomationPackageResourceProvider} deliberately does, would
 * silently discard every edit.
 * <p>
 * Any other {@code apId} is delegated, so that a package deployed on the same server keeps resolving
 * normally.
 */
public class LocalApResourceProvider implements ApResourceProvider {

    private final Supplier<Path> rootSupplier;
    private final ApResourceProvider deployedPackages;

    /**
     * @param rootSupplier     supplies the root directory of the automation package currently open in
     *                         the editor, or {@code null} if none is
     * @param deployedPackages the provider handling every other {@code apId}; may be {@code null} in a
     *                         distribution that has none, in which case such a reference fails
     */
    public LocalApResourceProvider(Supplier<Path> rootSupplier, ApResourceProvider deployedPackages) {
        this.rootSupplier = Objects.requireNonNull(rootSupplier, "rootSupplier must not be null");
        this.deployedPackages = deployedPackages;
    }

    @Override
    public File resolve(String apId, String relativePath) {
        if (!FileResolver.LOCAL_AP_ID.equals(apId)) {
            if (deployedPackages == null) {
                throw new RuntimeException("No provider is configured to resolve the resource " + relativePath
                    + " of the automation package " + apId);
            }
            return deployedPackages.resolve(apId, relativePath);
        }
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        Path root = currentAutomationPackageDirectory();
        // Normalising here rather than trusting the caller is what keeps an automation package
        // self-contained: the reference cannot address anything outside its own root.
        File file = root.resolve(FileResolver.normalizeApRelativePath(relativePath)).toFile();
        if (!file.exists()) {
            throw new ApResourceNotFoundException("The file '" + relativePath
                + "' does not exist in the automation package " + root);
        }
        return file;
    }

    /**
     * The package open in the editor is an exploded directory the user owns, so it is the one place
     * where a file <i>may</i> be created - which is what makes a keyword created from the editor land in
     * the automation package rather than in a Step resource.
     */
    @Override
    public Path getEditableRoot() {
        return currentAutomationPackageDirectory();
    }

    /**
     * @throws IllegalStateException if no automation package is open — the reference is fine, there is
     *                               simply nothing to resolve it against
     */
    private Path currentAutomationPackageDirectory() {
        Path root = rootSupplier.get();
        if (root == null) {
            throw new IllegalStateException("No automation package is currently open in the editor");
        }
        return root;
    }
}
