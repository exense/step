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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.Version;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.migration.MigrationContext;

/**
 * Since the schema version 1.3.0 the plan called by a {@code callPlan} is selected with the {@code plan} field, which
 * replaces the former {@code selectionAttributes} list of named values.
 * <p>
 * This task rewrites the selection attributes of the plans written against an earlier schema:
 * <ul>
 *     <li>a selection by name only becomes the simple form {@code plan: "My Plan"}</li>
 *     <li>any other selection becomes the map form {@code plan: {name: "My Plan", env: "T1"}}, keeping the order of
 *     the attributes. Dynamic values are kept as they are, both syntaxes share the same {@code expression} form</li>
 * </ul>
 */
@YamlPlanMigration
public class V1_3_0_CallPlanSelectionAttributesYamlMigrationTask extends AbstractYamlPlanMigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(V1_3_0_CallPlanSelectionAttributesYamlMigrationTask.class);

    private static final String ROOT = "root";
    private static final String CHILDREN = "children";
    private static final Set<String> CHILDREN_BLOCKS = Set.of("before", "after", "beforeThread", "afterThread");
    private static final String STEPS = "steps";

    private static final String CALL_PLAN = "callPlan";
    private static final String SELECTION_ATTRIBUTES = "selectionAttributes";
    private static final String PLAN = "plan";
    private static final String NAME = "name";

    public V1_3_0_CallPlanSelectionAttributesYamlMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(1, 3, 0), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        AtomicInteger migratedCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        try (Stream<Document> yamlPlans = yamlPlansCollection.findLazy(Filters.empty(), null, null, null, 0)) {
            yamlPlans.forEach(document -> {
                try {
                    DocumentObject root = document.getObject(ROOT);
                    if (root != null && migrateArtefact(root)) {
                        yamlPlansCollection.save(document);
                        migratedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.error("Unable to migrate the call plan selection attributes of the yaml plan {}", document, e);
                }
            });
        }

        logger.info("Migrated the call plan selection attributes of {} yaml plan(s)", migratedCount.get());
        if (errorCount.get() > 0) {
            logger.error("Failed to migrate the call plan selection attributes of {} yaml plan(s). Check the previous errors for details.", errorCount.get());
        }
    }

    /**
     * @param artefact an artefact node, which in the yaml syntax is a single entry whose key is the artefact name
     * @return true if any call plan was migrated
     */
    private boolean migrateArtefact(DocumentObject artefact) {
        boolean modified = false;
        // Should only contain one entry with our yaml syntax, but stay generic
        for (String artefactName : artefact.keySet()) {
            DocumentObject properties = artefact.getObject(artefactName);
            if (properties == null) {
                continue;
            }

            if (CALL_PLAN.equals(artefactName)) {
                modified |= migrateSelectionAttributes(properties);
            }

            modified |= migrateChildren(properties, CHILDREN);
            for (String childrenBlock : CHILDREN_BLOCKS) {
                DocumentObject block = properties.getObject(childrenBlock);
                if (block != null) {
                    modified |= migrateChildren(block, STEPS);
                }
            }
        }
        return modified;
    }

    private boolean migrateChildren(DocumentObject owner, String field) {
        List<DocumentObject> children = owner.getArray(field);
        if (children == null || children.isEmpty()) {
            return false;
        }
        boolean modified = false;
        for (DocumentObject child : children) {
            modified |= migrateArtefact(child);
        }
        if (modified) {
            // getArray returns a copy, it has to be set back explicitly
            owner.put(field, children);
        }
        return modified;
    }

    private boolean migrateSelectionAttributes(DocumentObject properties) {
        if (!properties.containsKey(SELECTION_ATTRIBUTES)) {
            return false;
        }
        List<DocumentObject> selectionAttributes = properties.getArray(SELECTION_ATTRIBUTES);
        properties.remove(SELECTION_ATTRIBUTES);
        if (selectionAttributes == null || selectionAttributes.isEmpty()) {
            return true;
        }

        DocumentObject criteria = new DocumentObject(new LinkedHashMap<>());
        for (DocumentObject attribute : selectionAttributes) {
            attribute.forEach(criteria::put);
        }

        Object name = criteria.get(NAME);
        if (criteria.size() == 1 && name instanceof String) {
            properties.put(PLAN, name);
        } else {
            properties.put(PLAN, criteria);
        }
        return true;
    }
}
