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

import step.core.GlobalContext;
import step.core.Version;
import step.core.collections.mongodb.MongoClientSession;

public abstract class MigrationTask {

	protected static final Logger logger = LoggerFactory.getLogger(MigrationTask.class);
	
	protected GlobalContext context;
	
	protected Version asOfVersion;
	
	protected MongoClientSession mongoClientSession;

	public GlobalContext getContext() {
		return context;
	}

	protected void setContext(GlobalContext context) {
		this.context = context;
		mongoClientSession = context.get(MongoClientSession.class);
	}

	public Version getAsOfVersion() {
		return asOfVersion;
	}

	/**
	 * @param asOfVersion the version as of which the the task has to be executed
	 */
	public MigrationTask(Version asOfVersion) {
		super();
		this.asOfVersion = asOfVersion;
	}
	
	/**
	 * Script to be executed when migrating from a version lower than asOfVersion to the version asOfVersion
	 */
	public abstract void runUpgradeScript();
	
	/**
	 * Script to be executed when migrating from the version asOfVersion to a version lower than asOfVersion
	 */
	public abstract void runDowngradeScript();

}
