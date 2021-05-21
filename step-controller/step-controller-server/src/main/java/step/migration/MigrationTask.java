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
package step.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.Version;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;

public abstract class MigrationTask {

	protected static final Logger logger = LoggerFactory.getLogger(MigrationTask.class);
	
	protected final Version asOfVersion;
	protected final MigrationContext migrationContext;
	protected final CollectionFactory collectionFactory;
	
	public MigrationTask(Version asOfVersion, CollectionFactory collectionFactory, MigrationContext migrationContext) {
		super();
		this.asOfVersion = asOfVersion;
		this.collectionFactory = collectionFactory;
		this.migrationContext = migrationContext;
	}

	public Version getAsOfVersion() {
		return asOfVersion;
	}
	
	/**
	 * Script to be executed when migrating from a version lower than asOfVersion to the version asOfVersion
	 */
	public abstract void runUpgradeScript();
	
	/**
	 * Script to be executed when migrating from the version asOfVersion to a version lower than asOfVersion
	 */
	public abstract void runDowngradeScript();

	protected Collection<Document> getDocumentCollection(String name) {
		return collectionFactory.getCollection(name, Document.class);
	}

}
