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

/**
 * In 3.18 a few entity names have been fixed in the UI. This task is
 * responsible for the renaming of these entities in the screen definitions
 *
 */
public class EntityRenamingMigrationTask extends MigrationTask {

	private Collection<Document> screenInputs;

	public EntityRenamingMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3, 18, 0), collectionFactory, migrationContext);
		screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		screenInputs.find(Filters.equals("screenId", "functionTable"), null, null, null, 0).forEach(t -> {
			DocumentObject input = t.getObject("input");

			String inputId = input.getString("id");
			if (inputId.equals("attributes.name")) {
				input.put("valueHtmlTemplate",
						"<entity-icon entity=\"stBean\" entity-name=\"'functions'\"/> <function-link function_=\"stBean\" st-options=\"stOptions\" />");
				screenInputs.save(t);
			}
		});
		screenInputs.find(Filters.equals("screenId", "parameterTable"), null, null, null, 0).forEach(t -> {
			DocumentObject input = t.getObject("input");

			String inputId = input.getString("id");
			if (inputId.equals("key")) {
				input.put("valueHtmlTemplate",
						"<entity-icon entity=\"stBean\" entity-name=\"'parameters'\"/> <parameter-key parameter=\"stBean\" st-options=\"stOptions\" />");
				screenInputs.save(t);
			}
		});
		screenInputs.find(Filters.equals("screenId", "schedulerTable"), null, null, null, 0).forEach(t -> {
			DocumentObject input = t.getObject("input");

			String inputId = input.getString("id");
			if (inputId.equals("attributes.name")) {
				input.put("valueHtmlTemplate",
						"<entity-icon entity=\"stBean\" entity-name=\"'tasks'\"/> <scheduler-task-link scheduler-task=\"stBean\" />");
				screenInputs.save(t);
			}
		});
	}

	@Override
	public void runDowngradeScript() {

	}

}
