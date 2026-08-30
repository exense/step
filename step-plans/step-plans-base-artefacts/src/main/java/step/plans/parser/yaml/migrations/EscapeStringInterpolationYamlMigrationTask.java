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

import java.util.List;
import java.util.Map;
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
import step.core.dynamicbeans.InterpolatedString;
import step.migration.MigrationContext;

/**
 * Since the schema version 1.3.0 the plain values of a plan may embed expressions using the {@code ${...}} placeholder
 * syntax. Values authored against an earlier schema were used literally, so any of them containing {@code ${} or
 * {@code $$} would change meaning, and would in most cases fail the execution with an unresolvable placeholder.
 * <p>
 * This task escapes those values so that a package written against schema 1.2.0 or earlier keeps behaving exactly as
 * it did. Authors opt into the interpolation by bumping the {@code version} of their descriptor and removing the
 * escaping where they actually want a placeholder.
 * <p>
 * <b>The field set below is frozen.</b> It describes where a value sits in the schemas up to 1.2.0, which is closed:
 * a document declaring such a version can never contain a field introduced later. It must therefore not be extended
 * when new fields appear - a later schema needing the same treatment gets its own task.
 * <p>
 * This first task deliberately covers only the fields where a placeholder is realistically found. The full inventory
 * of the values reaching a DynamicValue is wider, see the migration documentation.
 */
@YamlPlanMigration
public class EscapeStringInterpolationYamlMigrationTask extends AbstractYamlPlanMigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(EscapeStringInterpolationYamlMigrationTask.class);

    private static final String ROOT = "root";
    private static final String CHILDREN = "children";
    private static final Set<String> CHILDREN_BLOCKS = Set.of("before", "after", "beforeThread", "afterThread");
    private static final String STEPS = "steps";

    /**
     * The plain scalar fields of an artefact which end up in the value of a DynamicValue, by artefact yaml name
     */
    private static final Map<String, Set<String>> SCALAR_FIELDS = Map.of(
        "echo", Set.of("text"),
        "set", Set.of("key", "value")
    );

    /**
     * The fields holding a list of named values, each of which ends up in the value of a DynamicValue.
     * In yaml these are written as {@code - name: value} entries and are packed into a single json document by the
     * deserializer, so the values are escaped here individually
     */
    private static final Map<String, Set<String>> NAMED_VALUE_LISTS = Map.of(
        "callKeyword", Set.of("inputs"),
        "callPlan", Set.of("input")
    );

    public EscapeStringInterpolationYamlMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
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
                    if (root != null && escapeArtefact(root)) {
                        yamlPlansCollection.save(document);
                        migratedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.error("Unable to escape the string interpolation placeholders of the yaml plan {}", document, e);
                }
            });
        }

        logger.info("Escaped the string interpolation placeholders of {} yaml plan(s)", migratedCount.get());
        if (errorCount.get() > 0) {
            logger.error("Failed to escape {} yaml plan(s). Check the previous errors for details. The concerned plans " +
                "may contain values that are now interpreted as expressions.", errorCount.get());
        }
    }

    /**
     * @param artefact an artefact node, which in the yaml syntax is a single entry whose key is the artefact name
     * @return true if any value was escaped
     */
    private boolean escapeArtefact(DocumentObject artefact) {
        boolean modified = false;
        // Should only contain one entry with our yaml syntax, but stay generic
        for (String artefactName : artefact.keySet()) {
            DocumentObject properties = artefact.getObject(artefactName);
            if (properties == null) {
                continue;
            }

            for (String field : SCALAR_FIELDS.getOrDefault(artefactName, Set.of())) {
                modified |= escapeScalar(properties, field);
            }
            for (String field : NAMED_VALUE_LISTS.getOrDefault(artefactName, Set.of())) {
                modified |= escapeNamedValues(properties, field);
            }

            modified |= escapeChildren(properties, CHILDREN);
            for (String childrenBlock : CHILDREN_BLOCKS) {
                DocumentObject block = properties.getObject(childrenBlock);
                if (block != null) {
                    modified |= escapeChildren(block, STEPS);
                }
            }
        }
        return modified;
    }

    private boolean escapeChildren(DocumentObject owner, String field) {
        List<DocumentObject> children = owner.getArray(field);
        if (children == null || children.isEmpty()) {
            return false;
        }
        boolean modified = false;
        for (DocumentObject child : children) {
            modified |= escapeArtefact(child);
        }
        if (modified) {
            // getArray returns a copy, it has to be set back explicitly
            owner.put(field, children);
        }
        return modified;
    }

    /**
     * Escapes a plain scalar field. Values written as an expression are objects rather than strings and are left
     * untouched: they were already evaluated as groovy before the interpolation existed
     */
    private boolean escapeScalar(DocumentObject properties, String field) {
        Object value = properties.get(field);
        if (!(value instanceof String)) {
            return false;
        }
        String escaped = InterpolatedString.escape((String) value);
        if (escaped.equals(value)) {
            return false;
        }
        properties.put(field, escaped);
        return true;
    }

    /**
     * Escapes the values of a list of named values, for instance the keyword inputs
     */
    private boolean escapeNamedValues(DocumentObject properties, String field) {
        List<DocumentObject> namedValues = properties.getArray(field);
        if (namedValues == null || namedValues.isEmpty()) {
            return false;
        }
        boolean modified = false;
        for (DocumentObject namedValue : namedValues) {
            for (String name : namedValue.keySet()) {
                modified |= escapeScalar(namedValue, name);
            }
        }
        if (modified) {
            properties.put(field, namedValues);
        }
        return modified;
    }
}
