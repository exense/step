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

import step.core.Version;
import step.core.collections.*;
import step.core.plans.Plan;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrateFunctionCallsById extends MigrationTask {

    private final Collection<Document> functionCollection;
    private final Collection<Document> planCollection;

    public MigrateFunctionCallsById(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(3, 18, 0), collectionFactory, migrationContext);

        planCollection = collectionFactory.getCollection("plans", Document.class);
        functionCollection = collectionFactory.getCollection("functions", Document.class);
    }

    @Override
    public void runUpgradeScript() {
        migrateCallFunctionsById();
    }

    private void migrateCallFunctionsById() {
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger errorCount = new AtomicInteger();

        logger.info("Searching for plans referencing functions by ID to be migrated...");
        planCollection.find(Filters.equals("_class", Plan.class.getName()), null, null, null, 0).forEach(t -> {
            try {
                DocumentObject tree = t.getObject("root");

                boolean updated = handleArtefactTreeNode(tree);
                if (updated) {
                    planCollection.save(t);
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
                logger.error("Error while migrating plan " + t, e);
            }
        });
        logger.info("Migrated " + successCount.get() + " plans successfully.");
        if (errorCount.get() > 0) {
            logger.error("Got " + errorCount + " errors while migrating plans. See previous error logs for details.");
        }
    }

    private boolean handleArtefactTreeNode(DocumentObject tree) {
        boolean changed = false;
        if ("CallKeyword".equals(tree.getString("_class"))) {
            String functionId = tree.getString("functionId");
            if (functionId != null) {
                //lookup function
                Document function = functionCollection.find(Filters.id(functionId), null, null, null, 0).findFirst().orElse(null);
                JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
                if (function != null) {
                    DocumentObject attributes = function.getObject("attributes");
                    attributes.forEach((key, value) -> {
                        //attribute.
                        if (!key.equals("project")) {
                            objectBuilder.add(key, Json.createObjectBuilder().add("value", value.toString()).add("dynamic", false).build());
                        }
                    });
                }
                String functionAttributes = objectBuilder.build().toString();

                DocumentObject functionRef = new DocumentObject();
                functionRef.put("value", functionAttributes);
                functionRef.put("dynamic", false);

                tree.put("function", functionRef);
                tree.remove("functionId");
                changed = true;
            }
        }
        List<DocumentObject> children = tree.getArray("children");
        if(children != null) {
            int countOfChildrenChanged = children.stream().map(this::handleArtefactTreeNode).map(r -> r ? 1:0).reduce(0, Integer::sum);
            if (countOfChildrenChanged > 0) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void runDowngradeScript() {
    }
}
