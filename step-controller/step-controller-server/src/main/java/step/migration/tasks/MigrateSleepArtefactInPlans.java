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
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This function ensures that all the artefacts have their name saved properly in the attribute map. 
 * This will only be needed for the migration from 3.3.x or lower to 3.4.x or higher
 *
 */
public class MigrateSleepArtefactInPlans extends MigrationTask {

	private Collection<Document> planCollection;

	public MigrateSleepArtefactInPlans(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,25,0), collectionFactory, migrationContext);

		planCollection = collectionFactory.getCollection("plans", Document.class);
	}
	
	@Override
	public void runUpgradeScript() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Searching for artefacts of type 'Sleep' to be migrated...");
		
		planCollection.find(Filters.empty(), null, null, null, 0).forEach(p -> {
			try {
				List<DocumentObject> sleepNodesToBeUpdated = new ArrayList<>();
				
				retrieveSleepNodeRecursively(p.getObject("root"), sleepNodesToBeUpdated);
				
				sleepNodesToBeUpdated.forEach(sleepNode -> {
					try {
						logger.info("Migrating sleep node " + sleepNode.getString("_id") + ", found in plan " + p.getString("id") +
								" with name " + p.getObject("attributes").getString("name") );
						DocumentObject duration = sleepNode.getObject("duration");
						DocumentObject unit = sleepNode.getObject("unit");
						if (unit.getBoolean("dynamic")) {
							throw new UnsupportedOperationException("Cannot migrate Sleep control using dynamic expression for unit.");
						} else if (duration.getBoolean("dynamic") && (!unit.getString("value").equals("ms"))) {
							throw new UnsupportedOperationException("Cannot migrate Sleep control using dynamic expression for duration if the time unit is not milliseconds.");
						} else if (!duration.getBoolean("dynamic")) {
							Object o = duration.get("value");
							long sleepDuration = Long.parseLong(o.toString());
							String unitStr = unit.getString("value");
							if (unitStr.equals("s")) {
								sleepDuration *= 1000;
							} else if (unitStr.equals("m")) {
								sleepDuration *= 60000;
							}
							unit.put("value", "ms");
							duration.put("value", sleepDuration);

							planCollection.save(p);
						}
						//also successful for dynamic expression for duration and ms as unit (no migration required)
						successCount.incrementAndGet();
					} catch (UnsupportedOperationException e) {
						errorCount.incrementAndGet();
						logger.error("Error while migrating sleep control " + sleepNode.getString("_id") + ": " + e.getMessage());
					} catch (Exception e) {
						errorCount.incrementAndGet();
						logger.error("Error while migrating sleep control " + sleepNode.getString("_id") + ": ", e);
					}				
				});
			} catch(Exception e) {
				errorCount.incrementAndGet();
				logger.error("Error while migrating"
						+ " sleeps from plan " + p, e);
			}
		});	
		logger.info("Migrated "+successCount.get()+" sleep controls successfully.");
		if(errorCount.get()>0) {
			logger.error("Got "+errorCount+" errors while migrating sleep controls. See previous error logs for details.");
		}
	}
	
	private void retrieveSleepNodeRecursively(DocumentObject node, List<DocumentObject> sleepNodesToBeUpdated) {
		Object nodeClass = node.get("_class");
		if(nodeClass != null && nodeClass.toString().equals("Sleep")) {
			sleepNodesToBeUpdated.add(node);
		}
		
		List<DocumentObject> children = node.getArray("children");
		if(children != null) {
			children.forEach(child-> retrieveSleepNodeRecursively(child, sleepNodesToBeUpdated));
		}
	}
	
	@Override
	public void runDowngradeScript() {
		// TODO Auto-generated method stub		
	}
}
