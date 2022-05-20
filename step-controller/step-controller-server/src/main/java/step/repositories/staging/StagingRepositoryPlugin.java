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
package step.repositories.staging;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class StagingRepositoryPlugin extends AbstractControllerPlugin {

	public static final String STAGING_REPOSITORY = "staging";
	
	@Override
	public void serverStart(GlobalContext context) throws Exception {
		super.serverStart(context);
		
		StagingContextAccessorImpl registry = new StagingContextAccessorImpl(
				context.getCollectionFactory().getCollection("staging", StagingContext.class));
		StagingRepository repository = new StagingRepository(registry);
		
		context.getRepositoryObjectManager().registerRepository(STAGING_REPOSITORY, repository);
		
		context.put(StagingContextAccessorImpl.class, registry);
		context.getServiceRegistrationCallback().registerService(StagingRepositoryServices.class);
		
	}

}
