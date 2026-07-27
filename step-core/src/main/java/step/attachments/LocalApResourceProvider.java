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
package step.attachments;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@link ApResourceProvider} for the local IDE / exploded-folder mode ({@code apResource:local:...}).
 * The automation package is an unpacked folder on disk, so the entry is resolved directly against
 * that folder — no archive, no materialisation, no cache.
 */
public class LocalApResourceProvider implements ApResourceProvider {

    private final Supplier<Path> rootSupplier;

    /**
     * @param rootSupplier supplies the automation package root folder at resolve time (typically the
     *                     {@link FileResolver} unprefixed root set by the local IDE state)
     */
    public LocalApResourceProvider(Supplier<Path> rootSupplier) {
        this.rootSupplier = rootSupplier;
    }

    @Override
    public File resolve(String apId, String relativePath) {
        // In local mode the only valid apId is "local"; anything else is a wiring bug, not user input.
        if (!FileResolver.LOCAL_AP_ID.equals(apId)) {
            throw new IllegalArgumentException("LocalApResourceProvider only handles the '"
                    + FileResolver.LOCAL_AP_ID + "' apId, got: " + apId);
        }
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        String normalized = FileResolver.normalizeApRelativePath(relativePath);
        Path root = rootSupplier.get();
        if (root == null) {
            throw new RuntimeException("No local automation package root is configured to resolve apResource "
                    + relativePath);
        }
        File file = root.resolve(normalized).toFile();
        if (!file.exists()) {
            throw new ApResourceNotFoundException("Local apResource '" + relativePath + "' not found under " + root);
        }
        return file;
    }
}
