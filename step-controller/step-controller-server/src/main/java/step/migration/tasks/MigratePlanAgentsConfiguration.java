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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This function ensures that all the plans with auto scaling configuration in custom fields are migrated to the new model
 * This will only be needed for the migration from 3.25.x to 3.26.x or higher
 *
 */
public class MigratePlanAgentsConfiguration extends MigrationTask {

	public static final String AUTOSCALING_SETTINGS = "autoscalingSettings";

	private final Collection<Document> planCollection;

	public MigratePlanAgentsConfiguration(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,26,0), collectionFactory, migrationContext);

		planCollection = collectionFactory.getCollection("plans", Document.class);
	}
	
	@Override
	public void runUpgradeScript() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger migratedCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		logger.info("Searching for plans with auto scaling configurations to be migrated...");
		
		planCollection.find(Filters.empty(), null, null, null, 0).forEach(p -> {
			try {
				DocumentObject customFields = p.getObject("customFields");
				if (customFields != null) {
					//Custom fields are weirdly (de)serialized. object values are wrapped in an array with 1: the type info (String), 2: the object (map)
					Object wrappedCustomFieldValue = customFields.get(AUTOSCALING_SETTINGS);
					if (wrappedCustomFieldValue != null) {
						Map<String, Object> autoScalingSettings;
						try {
                            //noinspection unchecked
                            autoScalingSettings = (Map<String, Object>) ((List<Object>) wrappedCustomFieldValue).get(1);
						} catch (Exception e) {
							throw new RuntimeException("Found custom field for plan settings with incorrect content: " + wrappedCustomFieldValue);
						}
						if (autoScalingSettings != null) {
							customFields.remove(AUTOSCALING_SETTINGS);
							boolean enableAutoscaling = (boolean) autoScalingSettings.get("enableAutoscaling");
							boolean enableAutomaticTokenNumberCalculation = (boolean) autoScalingSettings.get("enableAutomaticTokenNumberCalculation");
							DocumentObject agents = new DocumentObject();
							if (enableAutoscaling) {
								if (enableAutomaticTokenNumberCalculation) {
									agents.put("mode", "auto_detect");
								} else {
									ArrayList<DocumentObject> configuredAgentPools = new ArrayList<>();
									//again custom field with map custom (de)serialization
									@SuppressWarnings("unchecked")
                                    List<Object> wrappedRequiredNumberOfTokens = (List<Object> ) autoScalingSettings.get("requiredNumberOfTokens");
									if (wrappedRequiredNumberOfTokens != null) {
										@SuppressWarnings("unchecked")
										Map<String, Integer> requiredNumberOfTokens = (Map<String, Integer>) wrappedRequiredNumberOfTokens.get(1);
										requiredNumberOfTokens.forEach((pool, replicas) -> {
											DocumentObject poolConfiguration = new DocumentObject();
											poolConfiguration.put("pool", pool);
											poolConfiguration.put("replicas", replicas);
											configuredAgentPools.add(poolConfiguration);
										});
									}
									agents.put("configuredAgentPools", configuredAgentPools);
								}
							} else {
								agents.put("configuredAgentPools", new ArrayList<>());
							}
							p.put("agents", agents);
							planCollection.save(p);
							migratedCount.incrementAndGet();
						}
					}
				}
				successCount.incrementAndGet();
			} catch (Exception e) {
				errorCount.incrementAndGet();
                logger.error("Unable to migrate agents configuration for plan: {}", p, e);
			}
		});

        logger.info("{} plans successfully processed. {} plans with agents configurations successfully.", successCount.get(), migratedCount.get());
		if(errorCount.get()>0) {
            logger.error("Got {} errors while migrating assert controls. See previous error logs for details.", errorCount);
		}
	}
	
	@Override
	public void runDowngradeScript() {
		// DowngradeScript not implemented
	}
}
