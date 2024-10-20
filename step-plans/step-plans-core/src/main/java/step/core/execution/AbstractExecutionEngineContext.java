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
package step.core.execution;

import ch.exense.commons.app.Configuration;
import step.core.AbstractStepContext;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.aggregated.ReportNodeTimeSeries;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNodeAccessor;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.InMemoryExecutionAccessor;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.PlanAccessor;
import step.core.repositories.RepositoryObjectManager;

public abstract class AbstractExecutionEngineContext extends AbstractStepContext {

	private OperationMode operationMode;
	private ArtefactHandlerRegistry artefactHandlerRegistry;

	private PlanAccessor planAccessor;
	private ReportNodeAccessor reportNodeAccessor;
	private ExecutionAccessor executionAccessor;

	private RepositoryObjectManager repositoryObjectManager;
	
	public AbstractExecutionEngineContext() {
		super();
	}

	protected void setDefaultAttributes() {
		super.setDefaultAttributes();
		setConfiguration(new Configuration());

		artefactHandlerRegistry = new ArtefactHandlerRegistry();

		planAccessor = new InMemoryPlanAccessor();
		reportNodeAccessor = new InMemoryReportNodeAccessor();
		executionAccessor = new InMemoryExecutionAccessor();

		repositoryObjectManager = new RepositoryObjectManager();

		InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(null);
		put(ResolvedPlanNodeAccessor.class, new ResolvedPlanNodeAccessor(collectionFactory));
		put(ReportNodeTimeSeries.class, new ReportNodeTimeSeries(collectionFactory, getConfiguration()));
	}
	
	protected void useAllAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
		super.useAllAttributesFromParentContext(parentContext);
		setConfiguration(parentContext.getConfiguration());

		operationMode = parentContext.getOperationMode();
		artefactHandlerRegistry = parentContext.getArtefactHandlerRegistry();

		planAccessor = parentContext.getPlanAccessor();
		reportNodeAccessor = parentContext.getReportNodeAccessor();
		executionAccessor = parentContext.getExecutionAccessor();

		repositoryObjectManager = parentContext.getRepositoryObjectManager();

        InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(null);
        inheritFromParentOrComputeIfAbsent(parentContext, ResolvedPlanNodeAccessor.class, x -> new ResolvedPlanNodeAccessor(collectionFactory));
        inheritFromParentOrComputeIfAbsent(parentContext, ReportNodeTimeSeries.class, x -> new ReportNodeTimeSeries(collectionFactory, parentContext.getConfiguration()));
	}

	public Configuration getConfiguration() {
		return this.get(Configuration.class);
	}

	public void setConfiguration(Configuration configuration) {
		this.put(Configuration.class, configuration);
	}

	public OperationMode getOperationMode() {
		return operationMode;
	}

	public void setOperationMode(OperationMode operationMode) {
		this.operationMode = operationMode;
	}

	public ArtefactHandlerRegistry getArtefactHandlerRegistry() {
		return artefactHandlerRegistry;
	}

	public void setArtefactHandlerRegistry(ArtefactHandlerRegistry artefactHandlerRegistry) {
		this.artefactHandlerRegistry = artefactHandlerRegistry;
	}

	public PlanAccessor getPlanAccessor() {
		return planAccessor;
	}
	
	public void setPlanAccessor(PlanAccessor planAccessor) {
		this.planAccessor = planAccessor;
	}
	
	public ReportNodeAccessor getReportAccessor() {
		return getReportNodeAccessor();
	}
	
	public ReportNodeAccessor getReportNodeAccessor() {
		return reportNodeAccessor;
	}
	
	public void setReportNodeAccessor(ReportNodeAccessor reportNodeAccessor) {
		this.reportNodeAccessor = reportNodeAccessor;
	}

	public ExecutionAccessor getExecutionAccessor() {
		return executionAccessor;
	}

	public void setExecutionAccessor(ExecutionAccessor executionAccessor) {
		this.executionAccessor = executionAccessor;
	}

	public RepositoryObjectManager getRepositoryObjectManager() {
		return repositoryObjectManager;
	}

	public void setRepositoryObjectManager(RepositoryObjectManager repositoryObjectManager) {
		this.repositoryObjectManager = repositoryObjectManager;
	}
}
