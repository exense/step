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
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

import java.util.List;

/**
 * This task migrates the screen inputs from screen 'functionTable' which
 * require the prefix "attributes." as of 3.11
 *
 */
public class ScreenEntityIconMigrationTask extends MigrationTask {

	private final Collection<Document> screenInputs;

	public ScreenEntityIconMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3, 23, 0), collectionFactory, migrationContext);
		screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		logger.info("Migrating default screen inputs with Entity Icons");

		screenInputs.find(Filters.empty(), null, null,null,0).forEach(d -> {
			String screenId = d.getString("screenId");
			DocumentObject input = d.getObject("input");
			String inputId = input.getString("id");
			if (screenId.equals("functionTable") && inputId.equals("attributes.name")) {
				input.put("customUIComponents", List.of("functionLink"));
			} else if (screenId.equals("planTable") && inputId.equals("attributes.name")) {
				input.put("customUIComponents", List.of("planLink"));
			} else if (screenId.equals("schedulerTable") && inputId.equals("attributes.name")) {
				input.put("customUIComponents", List.of("schedulerTaskLink"));
			} else if (screenId.equals("parameterTable") && inputId.equals("key")) {
				input.put("customUIComponents", List.of("parameterKey"));
			}
			screenInputs.save(d);
		});

		logger.info("Migrated screen inputs with Entity Icons");
	}

	@Override
	public void runDowngradeScript() {

	}
}
