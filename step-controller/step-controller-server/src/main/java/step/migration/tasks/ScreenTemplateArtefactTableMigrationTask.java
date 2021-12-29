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

import java.util.concurrent.atomic.AtomicBoolean;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.migration.MigrationContext;
import step.migration.MigrationTask;

/**
 * This task migrates the screen inputs from screen 'artefactTable' to 'planTable'
 *
 */
public class ScreenTemplateArtefactTableMigrationTask extends MigrationTask {

	private Collection<Document> screenInputs;

	public ScreenTemplateArtefactTableMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3,13,0), collectionFactory, migrationContext);
		screenInputs = collectionFactory.getCollection("screenInputs", Document.class);
	}

	@Override
	public void runUpgradeScript() {
		screenInputs.find(Filters.equals("screenId", "artefactTable"), null, null, null, 0).forEach(t -> {
			t.put("screenId", "planTable");
			DocumentObject input = t.getObject("input");
			
			String inputId = input.getString("id");
			if(inputId.equals("attributes.name")) {
				input.put("valueHtmlTemplate", "<plan-link entity-id=\"stBean.id\" description=\"stBean.attributes.name\" />");
			}
			
			if(!inputExist(inputId)) {
				screenInputs.save(t);
				logger.info("Migrated on screen input to "+t);
			} else {
				screenInputs.remove(Filters.id(t.getId()));
				logger.info("Deleted screen input");
			}

		});
	}

	protected boolean inputExist(String inputId) {
		AtomicBoolean result = new AtomicBoolean();
		screenInputs.find(Filters.equals("screenId", "planTable"), null, null, null, 0).forEach(doc -> {
			DocumentObject i = doc.getObject("input");
			if(i.getString("id").equals(inputId)) {
				result.set(true);
			}
		});
		return result.get();
	}
	
	@Override
	public void runDowngradeScript() {
		
	}

}
