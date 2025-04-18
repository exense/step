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

import org.bson.types.ObjectId;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plugins.ExecutionCallbacks;
import step.core.resolvers.Resolver;
import step.core.variables.VariablesManager;
import step.engine.execution.ExecutionManager;
import step.engine.execution.ExecutionVetoer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ExecutionContext extends AbstractExecutionEngineContext  {

	// Immutable fields
	private final String executionId;
	private final ExecutionParameters executionParameters;
	private final ArtefactHandlerManager artefactHandlerManager;
	private final ThreadLocal<ReportNode> currentNodeRegistry = new ThreadLocal<>();
	private final ReportNode reportNode;
	private final VariablesManager variablesManager;
	private final ReportNodeCache reportNodeCache;
	private final EventManager eventManager;
	private final ExecutionManager executionManager;
	private ExecutionCallbacks executionCallbacks;
	private final Resolver resolver;

	private ObjectEnricher objectEnricher;
	private ObjectPredicate objectPredicate;

	private final Set<ExecutionVetoer> executionVetoers = new HashSet<>();
	
	// Mutable fields
	private volatile ExecutionStatus status;
	private String executionType;
	private Plan plan;

	// Keep track of agents involved in the execution; access is via tailored methods,
	// so the implementation is not exposed via the "usual" getters.
	private final Set<String> agentUrls = ConcurrentHashMap.newKeySet();
	
	protected ExecutionContext(String executionId, ExecutionParameters executionParameters) {
		super();
		this.executionId = executionId;
		this.executionParameters = executionParameters;

		executionManager = new ExecutionManager(this);
		reportNodeCache = new ReportNodeCache();
		variablesManager = new VariablesManager(this);
		artefactHandlerManager = new ArtefactHandlerManager(this);
		eventManager = new EventManager();
		resolver = new Resolver();
		
		reportNode = new ReportNode();
		reportNode.setExecutionID(executionId);
		reportNode.setId(new ObjectId(executionId));
		reportNodeCache.put(reportNode);
		setCurrentReportNode(reportNode);
	}

	public ArtefactHandlerManager getArtefactHandlerManager() {
		return artefactHandlerManager;
	}

	public String getExecutionType() {
		return executionType;
	}

	public void setExecutionType(String executionType) {
		this.executionType = executionType;
	}

	@Override
	public void setExecutionAccessor(ExecutionAccessor executionAccessor) {
		super.setExecutionAccessor(executionAccessor);
	}

	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
	}

	public ReportNode getReport() {
		return reportNode;
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
	
	public void associateThread(long parentThreadId, ReportNode currentReportNode) {
		// TODO refactor the handling of current report node. It shouldn't be the responsability of the 
		// caller of this method to get the currentReportNode. We should be able to get it based on the parentThreadId
		setCurrentReportNode(currentReportNode);
		getExecutionCallbacks().associateThread(this, Thread.currentThread(),parentThreadId);
	}

	public String getExecutionId() {
		return executionId;
	}

	public ExecutionStatus getStatus() {
		return status;
	}

	private static final ThreadLocal<Boolean> interruptByPass = new ThreadLocal<>();

	public void byPassInterruptInCurrentThread(boolean bypass) {
		if (bypass) {
			interruptByPass.set(true);
		} else {
			interruptByPass.remove();
		}
	}
	public boolean isInterrupted() {
		boolean byPass = interruptByPass.get() != null && interruptByPass.get();
		return !byPass && (getStatus() == ExecutionStatus.ABORTING || getStatus() == ExecutionStatus.FORCING_ABORT);
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
	
	public EventManager getEventManager() {
		return eventManager;
	}

	public ExecutionManager getExecutionManager() {
		return executionManager;
	}

	public ExecutionCallbacks getExecutionCallbacks() {
		return executionCallbacks;
	}

	protected void setExecutionCallbacks(ExecutionCallbacks executionCallbacks) {
		this.executionCallbacks = executionCallbacks;
	}

	public ObjectEnricher getObjectEnricher() {
		return objectEnricher;
	}

	protected void setObjectEnricher(ObjectEnricher objectEnricher) {
		this.objectEnricher = objectEnricher;
	}

	public ObjectPredicate getObjectPredicate() {
		return objectPredicate;
	}

	protected void setObjectPredicate(ObjectPredicate objectPredicate) {
		this.objectPredicate = objectPredicate;
	}

	public Resolver getResolver() {
		return resolver;
	}

	public Set<ExecutionVetoer> getExecutionVetoers() {
		return new HashSet<>(executionVetoers);
	}

	public void addExecutionVetoer(ExecutionVetoer executionVetoer) {
		this.executionVetoers.add(executionVetoer);
	}

	public void addAgentUrl(String agentUrl) {
		if (agentUrl != null) {
			agentUrls.add(agentUrl);
		}
	}

	/**
	 * @return agents involved in the execution, as a single string of
	 * space-separated URLs.
	 */
	public String getAgentUrls() {
		return agentUrls.stream()
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.collect(Collectors.joining(" "));
	}
}
