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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import step.core.Version;
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.CollectionFactory;
import ch.exense.commons.core.collections.Document;
import ch.exense.commons.core.collections.DocumentObject;
import ch.exense.commons.core.collections.Filters;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

/**
 * This function ensures that all the artefacts have their name saved properly in the attribute map. 
 * This will only be needed for the migration from 3.3.x or lower to 3.4.x or higher
 *
 */
public class MigrateAssertNegation extends MigrationTask {
	
	private Collection<Document> planCollection;
	
	public MigrateAssertNegation(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,13,3), collectionFactory, migrationContext);

		planCollection = collectionFactory.getCollection("plans", Document.class);
	}
	
	@Override
	public void runUpgradeScript() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Searching for artefacts of type 'Assert' to be migrated...");
		
		planCollection.find(Filters.empty(), null, null, null, 0).forEach(p -> {
			try {
				List<DocumentObject> assertNodesToBeUpdated = new ArrayList<>();
				
				retrieveAssertNodeRecursively(p.getObject("root"), assertNodesToBeUpdated);
				
				assertNodesToBeUpdated.forEach(assertNode -> {
					try {
						if(assertNode.containsKey("negate")) {
							logger.info("Migrating assert node " + assertNode.getString("_id") + ", found in plan " + p.getString("_id"));
							boolean currentNegateValue = assertNode.getBoolean("negate");					
							assertNode.remove("negate");
							
							Map<String, Object> doNegateMap = new HashMap<String,Object>();
							doNegateMap.put("dynamic", false);
							doNegateMap.put("value", currentNegateValue);
							assertNode.put("doNegate", doNegateMap);
							
							planCollection.save(p);
							
							successCount.incrementAndGet();
						}						
					} catch (Exception e) {
						errorCount.incrementAndGet();
						logger.error("Error while migrating assert " + assertNode, e);
					}				
				});
			} catch(Exception e) {
				errorCount.incrementAndGet();
				logger.error("Error while migrating"
						+ " asserts from plan " + p, e);
			}
		});	
		logger.info("Migrated "+successCount.get()+" assert controls successfully.");
		if(errorCount.get()>0) {
			logger.error("Got "+errorCount+" errors while migrating assert controls. See previous error logs for details.");
		}
	}
	
	private void retrieveAssertNodeRecursively(DocumentObject node, List<DocumentObject> assertNodesToBeUpdated) {
		Object nodeClass = node.get("_class");
		if(nodeClass != null && nodeClass.toString().equals("Assert")) {				
			assertNodesToBeUpdated.add(node);
		}
		
		List<DocumentObject> children = node.getArray("children");
		if(children != null) {
			children.forEach(child->retrieveAssertNodeRecursively(child, assertNodesToBeUpdated));
		}
	}
	
	@Override
	public void runDowngradeScript() {
		// TODO Auto-generated method stub		
	}
}
