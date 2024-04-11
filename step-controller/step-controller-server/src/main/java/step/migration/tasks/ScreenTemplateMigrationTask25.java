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
import step.plugins.screentemplating.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * with Step 25, configuration of tables columns is changing and legacy screen templates which were only used for
 * this purpose are deprecated.
 * Besides attributes.name for function and plan templates become immutable
 *
 */
public class ScreenTemplateMigrationTask25 extends MigrationTask {

	private final Collection<Document> screenInputsCollection;
	private final ScreenInputAccessorImpl screenInputAccessor;

	public ScreenTemplateMigrationTask25(CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super(new Version(3, 25, 0), collectionFactory, migrationContext);
		screenInputsCollection = collectionFactory.getCollection("screenInputs", Document.class);
		screenInputAccessor = new ScreenInputAccessorImpl(
				collectionFactory.getCollection("screenInputs", ScreenInput.class));
	}

	@Override
	public void runUpgradeScript() {
		createImmutableNameInputAndRenameScreenId("functionTable", "keyword", "functionLink");
		createImmutableNameInputAndRenameScreenId("planTable", "plan", "planLink");

		logger.info("Cleaning up screen inputs which are replaced by the new table and columns configurations mechanism.");
		screenInputsCollection.remove(Filters.equals("screenId", "executionTable"));
		screenInputsCollection.remove(Filters.equals("screenId", "schedulerTable"));
		screenInputsCollection.remove(Filters.equals("screenId", "parameterDialog"));
		screenInputsCollection.remove(Filters.equals("screenId", "parameterTable"));
		screenInputsCollection.remove(Filters.equals("screenId", "functionTableExtensions"));
	}

	private void createImmutableNameInputAndRenameScreenId(String screeenId, String newScreenId, String customUiComponent) {
		List<ScreenInput> screenInputsByScreenId = screenInputAccessor.getScreenInputsByScreenId(screeenId);
		Input nameInput = new Input(InputType.TEXT, "attributes.name", "Name", null, null);
		nameInput.setCustomUIComponents(List.of(customUiComponent));
		AtomicBoolean inputExists = new AtomicBoolean(false);
		// Force content of input 'attributes.name'
		screenInputsByScreenId.forEach(i->{
			Input input = i.getInput();
			if(input.getId().equals("attributes.name")) {
				i.setInput(nameInput);
				i.setImmutable(true);
				inputExists.set(true);
			}
			i.setScreenId(newScreenId);
			screenInputAccessor.save(i);
		});
		// Create it if not existing
		if(!inputExists.get()) {
			screenInputAccessor.save(new ScreenInput(0, newScreenId, nameInput, true));
		}
	}

	@Override
	public void runDowngradeScript() {

	}
}
