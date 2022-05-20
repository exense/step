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
package step.core;

import step.core.access.UserAccessor;
import step.core.collections.CollectionFactory;
import step.core.entities.EntityManager;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.ControllerPluginManager;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.ExecutionTaskAccessor;
import step.framework.server.ServiceRegistrationCallback;

@OverrideServerContext
public class GlobalContext extends AbstractExecutionEngineContext {

	public GlobalContext() {
		super();
	}

	public CollectionFactory getCollectionFactory() {
		return this.get(CollectionFactory.class);
	}

	public void setCollectionFactory(CollectionFactory collectionFactory) {
		this.put(CollectionFactory.class,collectionFactory);
	}

	public ExecutionAccessor getExecutionAccessor() {
		return this.get(ExecutionAccessor.class);
	}

	public void setExecutionAccessor(ExecutionAccessor executionAccessor) {
		this.put(ExecutionAccessor.class,executionAccessor);
	}

	public ExecutionTaskAccessor getScheduleAccessor() {
		return this.get(ExecutionTaskAccessor.class);
	}

	public void setScheduleAccessor(ExecutionTaskAccessor scheduleAccessor) {
		this.put(ExecutionTaskAccessor.class,scheduleAccessor);
	}

	public UserAccessor getUserAccessor() {
		return this.get(UserAccessor.class);
	}

	public void setUserAccessor(UserAccessor userAccessor) {
		this.put(UserAccessor.class,userAccessor);
	}

	public ControllerPluginManager getControllerPluginManager() {
		return this.get(ControllerPluginManager.class);
	}

	public void setContorllerPluginManager(ControllerPluginManager pluginManager) {
		this.put(ControllerPluginManager.class,pluginManager);
	}
	
	public ServiceRegistrationCallback getServiceRegistrationCallback() {
		return this.get(ServiceRegistrationCallback.class);
	}

	public void setServiceRegistrationCallback(
			ServiceRegistrationCallback serviceRegistrationCallback) {
		this.put(ServiceRegistrationCallback.class,serviceRegistrationCallback);
	}

	public Version getCurrentVersion() {
		return this.require(Version.class);
	}

	public EntityManager getEntityManager() {
		return this.get(EntityManager.class);
	}

	public void setEntityManager(EntityManager entityManager) {
		this.put(EntityManager.class,entityManager);
	}

	public ExecutionScheduler getScheduler() {
		return this.get(ExecutionScheduler.class);
	}

	public void setScheduler(ExecutionScheduler scheduler) {
		this.put(ExecutionScheduler.class,scheduler);
	}
}
