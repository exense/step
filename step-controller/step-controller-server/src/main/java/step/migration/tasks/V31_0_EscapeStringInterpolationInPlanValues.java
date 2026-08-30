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
package step.migration.tasks;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.dynamicbeans.StringInterpolationEscaper;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import static step.core.collections.CollectionFactory.VERSION_COLLECTION_SUFFIX;

/**
 * Since Step 3.31 the plain (non dynamic) string values of the plans and parameters may embed expressions using the
 * {@code ${...}} placeholder syntax. Values authored before that were used literally, so any of them containing
 * {@code ${} would change meaning, and would in most cases fail the execution with an unresolvable
 * placeholder.
 * <p>
 * This task escapes all such values so that they keep resolving to exactly what they were before the upgrade. Users
 * who want a value to be interpolated remove the escaping themselves.
 * <p>
 * The same transformation is applied to the automation packages by the corresponding YAML migration, which runs when
 * a package declaring an older schema version is deployed or executed.
 */
public class V31_0_EscapeStringInterpolationInPlanValues extends MigrationTask {

    private static final String PLANS = "plans";
    private static final String FUNCTIONS = "functions";
    private static final String PARAMETERS = "parameters";

    public V31_0_EscapeStringInterpolationInPlanValues(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3, 31, 0), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        // Composite keywords embed a plan, and the versioned collections hold the history of both
        for (String collectionName : List.of(PLANS, PLANS + VERSION_COLLECTION_SUFFIX,
            FUNCTIONS, FUNCTIONS + VERSION_COLLECTION_SUFFIX, PARAMETERS)) {
            escapeCollection(collectionName);
        }
    }

    private void escapeCollection(String collectionName) {
        Collection<Document> collection = collectionFactory.getCollection(collectionName, Document.class);
        AtomicLong migratedCount = new AtomicLong();
        AtomicLong errorCount = new AtomicLong();

        logger.info("Escaping the string interpolation placeholders of the plain values in '{}'...", collectionName);
        try (Stream<Document> documents = collection.findLazy(Filters.empty(), null, null, null, 0)) {
            documents.forEach(document -> {
                try {
                    if (StringInterpolationEscaper.escapeDocument(document)) {
                        collection.save(document);
                        migratedCount.incrementAndGet();
                        logger.info("Escaped the plain values of the entry {} of '{}'", document.getId(), collectionName);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    logger.error("Error while escaping the plain values of the entry {} of '{}'", document.getId(), collectionName, e);
                }
            });
        }

        logger.info("Escaped the plain values of {} entries of '{}'", migratedCount.get(), collectionName);
        if (errorCount.get() > 0) {
            logger.error("Got {} errors while escaping the plain values of '{}'. See the previous error logs for details. " +
                "The concerned entries may contain values that are now interpreted as expressions.", errorCount.get(), collectionName);
        }
    }

    @Override
    public void runDowngradeScript() {
    }
}
