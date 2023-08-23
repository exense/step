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
package step.plans.parser.yaml.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.migration.MigrationContext;
import step.plans.parser.yaml.YamlPlanFields;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

@YamlPlanMigration
public class TestYamlPlanMigrationTaskV0 extends AbstractYamlPlanMigrationTask {

    private static final Logger log = LoggerFactory.getLogger(TestYamlPlanMigrationTaskV0.class);

    public TestYamlPlanMigrationTaskV0(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(1, 0, 0), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        Stream<Document> allYamlPlans = yamlPlansCollection.find(Filters.empty(), null, null, null, 0);
        allYamlPlans.forEach(new Consumer<>() {
            @Override
            public void accept(Document document) {
                DocumentObject root = document.getObject("root");
                renameArtefacts(root);
                yamlPlansCollection.save(document);
            }
        });
    }

    private void renameArtefacts(DocumentObject artifact) {
        Set<String> artefactNames = artifact.keySet();

        // set with only 1 object (artifactName)
        for (String artefactName : artefactNames) {
            String newName = renameArtifact(artefactName);
            DocumentObject artifactProperties = artifact.getObject(artefactName);
            if (newName != null) {
                log.info("Rename {} artifact to {}", artefactName, newName);
                artifact.put(newName, artifactProperties);
                artifact.remove(artefactName);
            }

            List<DocumentObject> children = artifactProperties.getArray(YamlPlanFields.ARTEFACT_CHILDREN);
            if (children != null && !children.isEmpty()) {
                for (DocumentObject child : children) {
                    renameArtefacts(child);
                }
            }
        }
    }

    private String renameArtifact(String oldName) {
        // test migration - rename artifact
        if(Objects.equals(oldName, "oldSequence")){
            return "sequence";
        } else if (Objects.equals(oldName, "oldAssert")) {
            return "assert";
        }
        return null;
    }
}
