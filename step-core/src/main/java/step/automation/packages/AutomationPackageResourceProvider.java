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

import step.automation.packages.accessor.AutomationPackageAccessor;

import java.io.File;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link ApResourceProvider} for MAIN and ISOLATED executions. Resolves an {@code apId} to its
 * {@link AutomationPackage} entity, obtains the archive resource, resolves it to the archive file,
 * opens the archive and lazily materialises the requested entry via {@link ApResourceMaterializer}.
 * <p>
 * The {@link AutomationPackageAccessor} and the archive-file resolution are supplied lazily
 * ({@link Supplier} / {@link Function}) because the accessor is only placed into the execution
 * context after the {@link step.attachments.FileResolver} is created, and — for isolated executions —
 * points to a different (layered) accessor than the global one.
 * <p>
 * The {@code archiveFactory} builds an {@link AutomationPackageArchive} from the resolved file; it is
 * injected rather than hardcoded so the concrete archive type is chosen by the caller (e.g. the
 * reader registry) rather than baked into this provider.
 */
public class AutomationPackageResourceProvider implements ApResourceProvider {

    private final File cacheRoot;
    private final Supplier<AutomationPackageAccessor> accessorSupplier;
    private final Function<String, File> archiveFileResolver;
    private final Function<File, AutomationPackageArchive> archiveFactory;
    private final ApResourceMaterializer materializer;

    /**
     * @param cacheRoot           the materialisation root (e.g. {@code data/AP_cache})
     * @param accessorSupplier    supplies the automation package accessor at resolve time
     * @param archiveFileResolver resolves the archive resource reference (a {@code resource:<id>}
     *                            string) to the archive file on disk
     * @param archiveFactory      opens an {@link AutomationPackageArchive} from the resolved archive
     *                            file
     */
    public AutomationPackageResourceProvider(File cacheRoot,
                                             Supplier<AutomationPackageAccessor> accessorSupplier,
                                             Function<String, File> archiveFileResolver,
                                             Function<File, AutomationPackageArchive> archiveFactory) {
        this(cacheRoot, accessorSupplier, archiveFileResolver, archiveFactory, new ApResourceMaterializer());
    }

    AutomationPackageResourceProvider(File cacheRoot,
                                      Supplier<AutomationPackageAccessor> accessorSupplier,
                                      Function<String, File> archiveFileResolver,
                                      Function<File, AutomationPackageArchive> archiveFactory,
                                      ApResourceMaterializer materializer) {
        this.cacheRoot = Objects.requireNonNull(cacheRoot, "cacheRoot must not be null");
        this.accessorSupplier = Objects.requireNonNull(accessorSupplier, "accessorSupplier must not be null");
        this.archiveFileResolver = Objects.requireNonNull(archiveFileResolver, "archiveFileResolver must not be null");
        this.archiveFactory = Objects.requireNonNull(archiveFactory, "archiveFactory must not be null");
        this.materializer = Objects.requireNonNull(materializer, "materializer must not be null");
    }

    @Override
    public File resolve(String apId, String relativePath) {
        Objects.requireNonNull(apId, "apId must not be null");
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        return materializer.materialize(cacheRoot, apId, relativePath,
            () -> archiveFactory.apply(resolveArchiveFile(apId)));
    }

    private File resolveArchiveFile(String apId) {
        AutomationPackageAccessor accessor = accessorSupplier.get();
        if (accessor == null) {
            throw new RuntimeException("No AutomationPackageAccessor is available to resolve the archive of automation package " + apId);
        }
        AutomationPackage automationPackage = accessor.get(apId);
        if (automationPackage == null) {
            throw new ApResourceNotFoundException("Automation package " + apId + " was not found");
        }
        String archiveReference = automationPackage.getAutomationPackageResource();
        if (archiveReference == null) {
            throw new ApResourceNotFoundException("Automation package " + apId + " has no archive resource");
        }
        File archiveFile = archiveFileResolver.apply(archiveReference);
        if (archiveFile == null || !archiveFile.exists()) {
            throw new ApResourceNotFoundException("The archive of automation package " + apId
                + " could not be resolved from reference " + archiveReference);
        }
        return archiveFile;
    }
}
