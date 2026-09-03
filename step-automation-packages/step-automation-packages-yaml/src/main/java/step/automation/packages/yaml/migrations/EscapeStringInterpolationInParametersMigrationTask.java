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
package step.automation.packages.yaml.migrations;

import java.util.List;
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
 * Since the schema version 1.3.0 the plain values of an automation package may embed expressions using the
 * {@code ${...}} placeholder syntax. Values authored against an earlier schema were used literally, so any of them
 * containing {@code ${} would change meaning, and would in most cases fail the execution with an unresolvable
 * placeholder.
 * <p>
 * This task escapes the parameter values of a descriptor or fragment declaring an older version, so that it keeps
 * behaving exactly as it did. The values contained in the plans of the package are escaped by the corresponding
 * migration of the yaml plan format.
 * <p>
 * <b>The field set is frozen.</b> It describes the parameters as they exist in the schemas up to 1.2.0, which is a
 * closed set: a document declaring such a version can never contain a field introduced later.
 */
@AutomationPackageMigration
public class EscapeStringInterpolationInParametersMigrationTask extends AbstractAutomationPackageMigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(EscapeStringInterpolationInParametersMigrationTask.class);

    private static final String PARAMETERS = "parameters";
    private static final String VALUE = "value";

    public EscapeStringInterpolationInParametersMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(1, 3, 0), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        AtomicInteger migratedCount = new AtomicInteger();

        try (Stream<Document> descriptors = descriptorsCollection.findLazy(Filters.empty(), null, null, null, 0)) {
            descriptors.forEach(descriptor -> {
                if (escapeParameterValues(descriptor)) {
                    descriptorsCollection.save(descriptor);
                    migratedCount.incrementAndGet();
                }
            });
        }

        if (migratedCount.get() > 0) {
            logger.info("Escaped the string interpolation placeholders of the parameters of {} automation package file(s)", migratedCount.get());
        }
    }

    private boolean escapeParameterValues(DocumentObject descriptor) {
        List<DocumentObject> parameters = descriptor.getArray(PARAMETERS);
        if (parameters == null || parameters.isEmpty()) {
            return false;
        }
        boolean modified = false;
        for (DocumentObject parameter : parameters) {
            Object value = parameter.get(VALUE);
            // Values written as an expression are objects rather than strings and are left untouched: they were
            // already evaluated as groovy before the interpolation existed
            if (value instanceof String) {
                String escaped = InterpolatedString.escape((String) value);
                if (!escaped.equals(value)) {
                    parameter.put(VALUE, escaped);
                    modified = true;
                }
            }
        }
        if (modified) {
            // getArray returns a copy of the list, it has to be set back explicitly
            descriptor.put(PARAMETERS, parameters);
        }
        return modified;
    }
}
