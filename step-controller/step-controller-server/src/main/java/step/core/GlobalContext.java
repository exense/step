/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.core;

import step.commons.conf.Configuration;
import step.core.Controller.ServiceRegistrationCallback;
import step.core.access.UserAccessor;
import step.core.accessors.MongoClientSession;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.ArtefactManager;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.execution.EventManager;
import step.core.execution.model.ExecutionAccessor;
import step.core.plugins.PluginManager;
import step.core.repositories.RepositoryObjectManager;
import step.core.scheduler.ExecutionTaskAccessor;
import step.expressions.ExpressionHandler;

public class GlobalContext extends AbstractContext {
	
	private Configuration configuration; 
	
	private PluginManager pluginManager;
	
	private RepositoryObjectManager repositoryObjectManager;
	
	private MongoClientSession mongoClientSession;
	
	private ExecutionAccessor executionAccessor;
	
	private ArtefactAccessor artefactAccessor;
	
	private ArtefactManager artefactManager;
	
	private ReportNodeAccessor reportAccessor;
	
	private ExecutionTaskAccessor scheduleAccessor;
	
	private UserAccessor userAccessor;
	
	private ServiceRegistrationCallback serviceRegistrationCallback;
	
	private ExpressionHandler expressionHandler;
	
	private DynamicBeanResolver dynamicBeanResolver;
	
	private EventManager eventManager;
	
	public GlobalContext() {
		super();
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public MongoClientSession getMongoClientSession() {
		return mongoClientSession;
	}

	public void setMongoClientSession(MongoClientSession mongoClientSession) {
		this.mongoClientSession = mongoClientSession;
	}

	public ExecutionAccessor getExecutionAccessor() {
		return executionAccessor;
	}

	public void setExecutionAccessor(ExecutionAccessor executionAccessor) {
		this.executionAccessor = executionAccessor;
	}

	public ArtefactAccessor getArtefactAccessor() {
		return artefactAccessor;
	}

	public void setArtefactAccessor(ArtefactAccessor artefactAccessor) {
		this.artefactAccessor = artefactAccessor;
	}

	public ArtefactManager getArtefactManager() {
		return artefactManager;
	}

	public void setArtefactManager(ArtefactManager artefactManager) {
		this.artefactManager = artefactManager;
	}

	public ReportNodeAccessor getReportAccessor() {
		return reportAccessor;
	}

	public void setReportAccessor(ReportNodeAccessor reportAccessor) {
		this.reportAccessor = reportAccessor;
	}

	public ExecutionTaskAccessor getScheduleAccessor() {
		return scheduleAccessor;
	}

	public void setScheduleAccessor(ExecutionTaskAccessor scheduleAccessor) {
		this.scheduleAccessor = scheduleAccessor;
	}

	public UserAccessor getUserAccessor() {
		return userAccessor;
	}

	public void setUserAccessor(UserAccessor userAccessor) {
		this.userAccessor = userAccessor;
	}

	public PluginManager getPluginManager() {
		return pluginManager;
	}

	public void setPluginManager(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}

	public RepositoryObjectManager getRepositoryObjectManager() {
		return repositoryObjectManager;
	}

	public void setRepositoryObjectManager(
			RepositoryObjectManager repositoryObjectManager) {
		this.repositoryObjectManager = repositoryObjectManager;
	}
	
	public ServiceRegistrationCallback getServiceRegistrationCallback() {
		return serviceRegistrationCallback;
	}

	public void setServiceRegistrationCallback(
			ServiceRegistrationCallback serviceRegistrationCallback) {
		this.serviceRegistrationCallback = serviceRegistrationCallback;
	}
	
	public ExpressionHandler getExpressionHandler() {
		return expressionHandler;
	}

	public void setExpressionHandler(ExpressionHandler expressionHandler) {
		this.expressionHandler = expressionHandler;
	}

	public DynamicBeanResolver getDynamicBeanResolver() {
		return dynamicBeanResolver;
	}

	public void setDynamicBeanResolver(DynamicBeanResolver dynamicBeanResolver) {
		this.dynamicBeanResolver = dynamicBeanResolver;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public void setEventManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}
	
	public Version getCurrentVersion() {
		// TODO read this from manifest
		return new Version(3,9,0);
	}
	
}
