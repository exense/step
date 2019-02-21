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
package step.core.execution;

import step.commons.conf.Configuration;
import step.core.AbstractContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.plugins.ExecutionCallbacks;
import step.core.repositories.RepositoryObjectManager;
import step.core.variables.VariablesManager;
import step.expressions.ExpressionHandler;

public class ExecutionContext extends AbstractContext  {
		
	private ThreadLocal<ReportNode> currentNodeRegistry = new ThreadLocal<>();

	private ExecutionParameters executionParameters;
	
	private final String executionId;
	
	private AbstractArtefact artefact;

	private ReportNode report;
	
	private ExecutionStatus status;
	
	private final VariablesManager variablesManager;
				
	private final ReportNodeCache reportNodeCache;
	
	private final ArtefactCache artefactCache;
	
	private ArtefactAccessor artefactAccessor;
	
	private ReportNodeAccessor reportNodeAccessor;
	
	private ExecutionAccessor executionAccessor;
	
	private ExpressionHandler expressionHandler;
	
	private DynamicBeanResolver dynamicBeanResolver;
	
	private EventManager eventManager;
	
	private ExecutionCallbacks executionCallbacks;
	
	private Configuration configuration;
	
	private RepositoryObjectManager repositoryObjectManager;
	
	public ExecutionContext(String executionId) {
		super();
				
		this.executionId = executionId;
				
		reportNodeCache = new ReportNodeCache();
		artefactCache = new ArtefactCache();
		variablesManager = new VariablesManager(this);
	}

	public AbstractArtefact getArtefact() {
		return artefact;
	}

	public void setArtefact(AbstractArtefact artefact) {
		this.artefact = artefact;
	}

	public ReportNode getReport() {
		return report;
	}

	public void setReport(ReportNode report) {
		this.report = report;
	}
	
	public ReportNodeCache getReportNodeCache() {
		return reportNodeCache;
	}

	public ReportNode getCurrentReportNode() {
		ReportNode currentNode = currentNodeRegistry.get();
		if(currentNode==null) {
			throw new RuntimeException("Current report node is null!");
		} else {
			return currentNode;
		}
	}
	
	public void setCurrentReportNode(ReportNode node) {
		currentNodeRegistry.set(node);
	}
	
	public void associateThread() {
		getExecutionCallbacks().associateThread(this, Thread.currentThread());
	}

	public String getExecutionId() {
		return executionId;
	}

	public ExecutionStatus getStatus() {
		return status;
	}
	
	public boolean isInterrupted() {
		return getStatus() == ExecutionStatus.ABORTING;
	}

	public boolean isSimulation() {
		return getExecutionParameters().getMode()==ExecutionMode.SIMULATION;
	}
	
	public void updateStatus(ExecutionStatus status) {
		this.status = status;
	}
	
	public VariablesManager getVariablesManager() {
		return variablesManager;
	}

	@Override
	public String toString() {
		return "ExecutionContext [executionId=" + executionId + "]";
	}
	
	public ExecutionParameters getExecutionParameters() {
		return executionParameters;
	}

	public void setExecutionParameters(ExecutionParameters parameters) {
		this.executionParameters = parameters;
	}

	public ArtefactCache getArtefactCache() {
		return artefactCache;
	}

	public ArtefactAccessor getArtefactAccessor() {
		return artefactAccessor;
	}

	public ReportNodeAccessor getReportNodeAccessor() {
		return reportNodeAccessor;
	}

	public ExecutionAccessor getExecutionAccessor() {
		return executionAccessor;
	}
	
	public ExpressionHandler getExpressionHandler() {
		return expressionHandler;
	}
	
	public DynamicBeanResolver getDynamicBeanResolver() {
		return dynamicBeanResolver;
	}
	
	public EventManager getEventManager() {
		return eventManager;
	}

	public ExecutionCallbacks getExecutionCallbacks() {
		return executionCallbacks;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public RepositoryObjectManager getRepositoryObjectManager() {
		return repositoryObjectManager;
	}

	protected void setArtefactAccessor(ArtefactAccessor artefactAccessor) {
		this.artefactAccessor = artefactAccessor;
	}

	protected void setReportNodeAccessor(ReportNodeAccessor reportNodeAccessor) {
		this.reportNodeAccessor = reportNodeAccessor;
	}

	protected void setExecutionAccessor(ExecutionAccessor executionAccessor) {
		this.executionAccessor = executionAccessor;
	}

	protected void setExpressionHandler(ExpressionHandler expressionHandler) {
		this.expressionHandler = expressionHandler;
	}

	protected void setDynamicBeanResolver(DynamicBeanResolver dynamicBeanResolver) {
		this.dynamicBeanResolver = dynamicBeanResolver;
	}

	protected void setEventManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}

	protected void setExecutionCallbacks(ExecutionCallbacks executionCallbacks) {
		this.executionCallbacks = executionCallbacks;
	}

	protected void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	protected void setRepositoryObjectManager(RepositoryObjectManager repositoryObjectManager) {
		this.repositoryObjectManager = repositoryObjectManager;
	}
}
