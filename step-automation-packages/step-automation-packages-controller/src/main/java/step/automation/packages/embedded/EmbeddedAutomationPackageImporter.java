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
package step.automation.packages.embedded;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageFileSource;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageUpdateParameter;
import step.automation.packages.AutomationPackageUpdateParameterBuilder;
import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.JavaAutomationPackageArchive;
import step.core.AbstractContext;
import step.core.objectenricher.AttributeResolverRegistry;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.WriteAccessValidator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Deploys the automation packages shipped with the distribution, from
 * <ul>
 *     <li>{@code {folder}/local} - keywords executed on the controller</li>
 *     <li>{@code {folder}/remote} - keywords executed on an agent</li>
 * </ul>
 * <p>
 * Replaces the embedded keyword package import and keeps its behaviour: the folders are scanned on
 * every controller start and every archive found is deployed again, so a distribution shipping a new
 * build of an archive picks it up without any further action. A package whose archive is no longer in
 * the folder is left deployed.
 */
public class EmbeddedAutomationPackageImporter {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedAutomationPackageImporter.class);

    private static final String LOCAL_FOLDER = "local";
    private static final String REMOTE_FOLDER = "remote";
    private static final String SIDECAR_EXTENSION = ".json";
    private static final String IMPORT_ACTOR = "embedded-automation-packages";

    private final AutomationPackageManager automationPackageManager;
    private final ObjectHookRegistry objectHookRegistry;
    private final AttributeResolverRegistry attributeResolverRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmbeddedAutomationPackageImporter(AutomationPackageManager automationPackageManager,
                                             ObjectHookRegistry objectHookRegistry,
                                             AttributeResolverRegistry attributeResolverRegistry) {
        this.automationPackageManager = automationPackageManager;
        this.objectHookRegistry = objectHookRegistry;
        this.attributeResolverRegistry = attributeResolverRegistry;
    }

    /**
     * @return the ids of the deployed automation packages
     */
    public List<String> importEmbeddedAutomationPackages(String packageFolder) {
        List<String> deployed = new ArrayList<>();
        deployed.addAll(importFolder(new File(packageFolder, LOCAL_FOLDER), true));
        deployed.addAll(importFolder(new File(packageFolder, REMOTE_FOLDER), false));
        return deployed;
    }

    private List<String> importFolder(File folder, boolean executeLocally) {
        List<String> deployed = new ArrayList<>();
        File[] candidates = folder.listFiles();
        if (candidates == null) {
            logger.debug("The embedded automation package folder {} does not exist.", folder.getAbsolutePath());
        } else {
            logger.info("Importing the embedded automation packages of {}.", folder.getAbsolutePath());
            // Archives are recognized by their content rather than by their extension, which leaves
            // out the sidecars and anything else the folder happens to hold.
            Arrays.stream(candidates)
                    .filter(file -> file.isFile() && JavaAutomationPackageArchive.isValidForFile(file))
                    .forEach(archive -> {
                        try {
                            deployed.add(deploy(archive, executeLocally));
                        } catch (Exception e) {
                            logger.error("Unable to import the embedded automation package {}.",
                                    archive.getAbsolutePath(), e);
                        }
                    });
        }
        return deployed;
    }

    private String deploy(File archive, boolean executeLocally) throws Exception {
        EmbeddedAutomationPackageDescriptor descriptor = readSidecar(archive);

        AbstractContext enrichmentContext = new AbstractContext() {
        };
        objectHookRegistry.rebuildContext(enrichmentContext, descriptor);
        ObjectEnricher enricher = objectHookRegistry.getObjectEnricher(enrichmentContext);
        ObjectPredicate objectPredicate = objectHookRegistry.getObjectPredicate(enrichmentContext);

        try (InputStream content = new FileInputStream(archive)) {
            // The package is looked up by name within the predicate above, so one already deployed
            // from this archive is updated rather than duplicated, keeping its keyword ids.
            AutomationPackageUpdateParameter parameters = new AutomationPackageUpdateParameterBuilder()
                    .withApSource(AutomationPackageFileSource.withInputStream(content, archive.getName()))
                    .withExecuteFunctionsLocally(executeLocally)
                    .withEnricher(enricher)
                    .withObjectPredicate(objectPredicate)
                    .withWriteAccessValidator(WriteAccessValidator.NO_CHECKS_VALIDATOR)
                    .withActorUser(IMPORT_ACTOR)
                    .withAsync(false)
                    .build();

            AutomationPackageUpdateResult result = automationPackageManager.createOrUpdateAutomationPackage(parameters);
            logger.info("Imported the embedded automation package {} as {} ({}).",
                    archive.getName(), result.getId(), result.getStatus());
            if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
                logger.warn("The import of the embedded automation package {} reported: {}",
                        archive.getName(), String.join("; ", result.getWarnings()));
            }
            return result.getId().toString();
        }
    }

    /**
     * @return the resolved sidecar attributes, or an empty descriptor when the archive has no sidecar
     */
    private EmbeddedAutomationPackageDescriptor readSidecar(File archive) throws Exception {
        File sidecar = new File(archive.getAbsolutePath() + SIDECAR_EXTENSION);
        EmbeddedAutomationPackageDescriptor descriptor;
        if (sidecar.exists()) {
            try {
                descriptor = objectMapper.readValue(sidecar, EmbeddedAutomationPackageDescriptor.class);
            } catch (Exception e) {
                throw new Exception("Error while reading the sidecar " + sidecar.getAbsolutePath(), e);
            }
            descriptor.setAttributes(attributeResolverRegistry.resolveAll(descriptor.getAttributes()));
        } else {
            descriptor = new EmbeddedAutomationPackageDescriptor();
        }
        return descriptor;
    }
}
