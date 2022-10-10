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
package step.resources;

import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.entities.EntityManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;

@Plugin()
public class ResourceManagerControllerPlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(ResourceServices.class);
		
		Collection<Resource> collectionDriver = context.getCollectionFactory().getCollection(EntityManager.resources,
				Resource.class);
		context.get(TableRegistry.class).register(EntityManager.resources, new Table<>(collectionDriver, null, true));
	}

	public static String getResourceDir(Configuration configuration) {
		String resourceRootDir = configuration.getProperty("resources.dir","resources");
		return resourceRootDir;
	}
}
