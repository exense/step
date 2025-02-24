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
package step.core.artefacts.handlers;

import ch.exense.commons.app.Configuration;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactFilter;
import step.core.artefacts.ChildrenBlock;
import step.core.artefacts.WorkArtefactFactory;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.aggregated.ReportNodeTimeSeries;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanBuilder;
import step.core.artefacts.reports.resolvedplan.ResolvedChildren;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.execution.ReportNodeCache;
import step.core.execution.ReportNodeEventListener;
import step.core.functions.FunctionGroupHandle;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.miscellaneous.ValidationException;
import step.core.variables.VariablesManager;
import step.resources.ResourceManager;

import java.io.File;
import java.security.SecureRandom;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

public abstract class ArtefactHandler<ARTEFACT extends AbstractArtefact, REPORT_NODE extends ReportNode> {

	public static final String ARTEFACT_PATH = "artefactPath";
	protected static Logger logger = LoggerFactory.getLogger(ArtefactHandler.class);

	public static final String FILE_VARIABLE_PREFIX = "file:";
	// Flag used to force the persistence of report nodes before execution of the artefact and bypass
	// the persistbefore = false
	public static final String FORCE_PERSIST_BEFORE = "forcePersistBefore";
	public static final String TEC_EXECUTION_REPORTNODES_PERSISTAFTER = "tec.execution.reportnodes.persistafter";
	public static final String TEC_EXECUTION_REPORTNODES_PERSISTBEFORE = "tec.execution.reportnodes.persistbefore";
	public static final String TEC_EXECUTION_REPORTNODES_PERSISTONLYNONPASSED = "tec.execution.reportnodes.persistonlynonpassed";
	public static final String CTX_ADDITIONAL_ATTRIBUTES = "$additionalAttributes";

	protected ExecutionContext context;
	private ArtefactHandlerManager artefactHandlerManager;
	private ReportNodeAttachmentManager reportNodeAttachmentManager;
	private ReportNodeAttributesManager reportNodeAttributesManager;
	private WorkArtefactFactory workArtefactFactory = new WorkArtefactFactory();
	
	private ReportNodeAccessor reportNodeAccessor;
	private VariablesManager variablesManager;
	private ReportNodeCache reportNodeCache;
	protected DynamicBeanResolver dynamicBeanResolver;
	private ReportNodeTimeSeries reportNodeTimeSeries;
	private boolean reportNodeTimeSeriesEnabled;
	private ResolvedPlanBuilder resolvedPlanBuilder;

	public ArtefactHandler() {
		super();		
	}
	
	public void init(ExecutionContext context) {
		this.context = context;
		artefactHandlerManager = context.getArtefactHandlerManager();
		reportNodeAccessor = context.getReportNodeAccessor();
		reportNodeTimeSeries = context.require(ReportNodeTimeSeries.class);
		reportNodeCache = context.getReportNodeCache();
		variablesManager = context.getVariablesManager();
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(context);
		reportNodeAttributesManager = new ReportNodeAttributesManager(context);
		dynamicBeanResolver = context.getDynamicBeanResolver();
		resourceManager = context.getResourceManager();
		resolvedPlanBuilder = context.get(ResolvedPlanBuilder.class);
		Configuration configuration = context.getConfiguration();
	}
	
	private enum Phase {
		
		SKELETON_CREATION,
		
		EXECUTION;
	}
	
	public void createReportSkeleton(ReportNode parentReportNode, ARTEFACT artefact, Map<String, Object> newVariables, ParentSource parentSource) {
		REPORT_NODE reportNode = beforeDelegation(Phase.SKELETON_CREATION, parentReportNode, artefact, newVariables, parentSource);
		if(parentReportNode != null && parentReportNode.isOrphan()) {
			reportNode.setOrphan(true);
		} else {
			reportNode.setOrphan(!artefact.isCreateSkeleton());
		}

		try {
			dynamicBeanResolver.evaluate(artefact, getBindings());
			reportNode.setName(getReportNodeNameDynamically(artefact));
			if(filterArtefact(artefact)) {
				reportNode.setStatus(ReportNodeStatus.SKIPPED);
			} else {
				try {
					optionalRunChildrenBlock(artefact.getBefore(), (before) -> {
						SequentialArtefactScheduler sequentialArtefactScheduler = new SequentialArtefactScheduler(context);
						sequentialArtefactScheduler.createReportSkeleton_(reportNode, before.getSteps(), ParentSource.BEFORE);
					});
					createReportSkeleton_(reportNode, artefact);
				} finally {
					optionalRunChildrenBlock(artefact.getAfter(), (after) -> {
						SequentialArtefactScheduler sequentialArtefactScheduler = new SequentialArtefactScheduler(context);
						sequentialArtefactScheduler.createReportSkeleton_(reportNode, after.getSteps(), ParentSource.AFTER);
					});
				}
			}
		} catch (Throwable e) {
			getListOfArtefactsNotInitialized().add(artefact.getId().toString());
			failWithException(reportNode, e, false);
		}
		
		if(artefact.isCreateSkeleton() && !reportNode.isOrphan()) {
			saveReportNode(reportNode);
		}
		
		context.getExecutionCallbacks().afterReportNodeSkeletonCreation(context, reportNode);
		
		afterDelegation(reportNode, parentReportNode, artefact);
	}

	protected void optionalRunChildrenBlock(ChildrenBlock block, Consumer<ChildrenBlock> consumer) {
		if (block != null && block.getSteps() != null && !block.getSteps().isEmpty()) {
			consumer.accept(block);
		}
	}

	protected abstract void createReportSkeleton_(REPORT_NODE parentNode, ARTEFACT testArtefact);
	
	public ReportNode execute(REPORT_NODE parentReportNode, ARTEFACT artefact, Map<String, Object> newVariables, ParentSource parentSource) {
		// If the artefact hasn't been initialized during createReportSkeleton phase, relaunch the skeleton creation phase for this node
		if(getListOfArtefactsNotInitialized().contains(artefact.getId().toString())) {
			createReportSkeleton(parentReportNode, artefact, newVariables, parentSource);
		}

		REPORT_NODE reportNode = beforeDelegation(Phase.EXECUTION, parentReportNode, artefact, newVariables, parentSource);

		long t1 = System.currentTimeMillis();
		reportNode.setExecutionTime(t1);
		reportNode.setStatus(ReportNodeStatus.RUNNING);
		
		final boolean persistBefore, persistAfter;
		final boolean persistOnlyNonPassed = variablesManager.getVariableAsBoolean(TEC_EXECUTION_REPORTNODES_PERSISTONLYNONPASSED, false);
		if (persistOnlyNonPassed) {
			persistBefore = false;
			persistAfter = true;
		} else {
			persistBefore = variablesManager.getVariableAsBoolean(TEC_EXECUTION_REPORTNODES_PERSISTBEFORE, true);
			persistAfter = variablesManager.getVariableAsBoolean(TEC_EXECUTION_REPORTNODES_PERSISTAFTER, true);
		}

		try {
			dynamicBeanResolver.evaluate(artefact, getBindings());
			reportNode.setName(getReportNodeNameDynamically(artefact));
			reportNode.setArtefactInstance(artefact);
			reportNode.setResolvedArtefact(artefact);

			context.getExecutionCallbacks().beforeReportNodeExecution(context, reportNode);

			if (filterArtefact(artefact)) {
				reportNode.setStatus(ReportNodeStatus.SKIPPED);
			} else {
				Object forcePersistBefore = artefact.getCustomAttribute(FORCE_PERSIST_BEFORE);
				if(persistBefore || Boolean.TRUE.equals(forcePersistBefore)) {
					saveReportNode(reportNode);
				}

				AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(reportNode);
				try {
					optionalRunChildrenBlock(artefact.getBefore(), (before) -> {
						SequentialArtefactScheduler sequentialArtefactScheduler = new SequentialArtefactScheduler(context);
						dynamicBeanResolver.evaluate(before, getBindings());
						sequentialArtefactScheduler.execute_(reportNode, before.getSteps(), before.getContinueOnError().get(), ParentSource.BEFORE);
						reportNodeStatusComposer.addStatusAndRecompose(reportNode);
					});
					//Only execute if no before is defined (RUNNING) or before was successful (PASSED)
					if (reportNode.getStatus().equals(ReportNodeStatus.PASSED) || reportNode.getStatus().equals(ReportNodeStatus.RUNNING)) {
						execute_(reportNode, artefact);
						reportNodeStatusComposer.addStatusAndRecompose(reportNode);
					}
				} finally {
					// Execute the AfterSequence artefacts even when aborting
					boolean byPassInterrupt = context.isInterrupted();
					if (byPassInterrupt) {
						context.byPassInterruptInCurrentThread(true);
					}
					try{
						optionalRunChildrenBlock(artefact.getAfter(), (after) -> {
							SequentialArtefactScheduler sequentialArtefactScheduler = new SequentialArtefactScheduler(context);
							dynamicBeanResolver.evaluate(after, getBindings());
							sequentialArtefactScheduler.execute_(reportNode, after.getSteps(), after.getContinueOnError().get(), ParentSource.AFTER);
							reportNodeStatusComposer.addStatusAndRecompose(reportNode);
						});
					} finally {
						//resume aborting if required
						if (byPassInterrupt) {
							context.byPassInterruptInCurrentThread(false);
						}
					}
				}

				reportNodeStatusComposer.applyComposedStatusToParentNode(reportNode);
			}
		} catch (Throwable e) {
			failWithException(reportNode, e);
		}
		long duration = System.currentTimeMillis() - t1;
		
		reportNode.setDuration((int)duration);

		if(persistAfter) {
			if(!persistOnlyNonPassed){
				saveReportNode(reportNode);
			} else if(!reportNode.getStatus().equals(ReportNodeStatus.PASSED) &&
						!reportNode.getStatus().equals(ReportNodeStatus.SKIPPED)){
				saveReportNode(reportNode);
			}
		}

		if (reportNodeTimeSeries.isIngestionEnabled()) {
			AbstractArtefact artefactInstance = reportNode.getArtefactInstance();
			if (artefactInstance != null && !artefactInstance.isWorkArtefact()) {
				// TODO implement node pruning for time series
				Map<String, Object> customAttributes = getTimeSeriesContextAttributes(context);
				customAttributes.put("type", artefactInstance.getClass().getSimpleName());
				customAttributes.put("name", artefact.getAttributes().get(AbstractOrganizableObject.NAME));
				reportNodeTimeSeries.ingestReportNode(reportNode, customAttributes);
			}
		}

		context.getExecutionCallbacks().afterReportNodeExecution(context, reportNode);
		
		afterDelegation(reportNode, parentReportNode, artefact);
		
		return reportNode;
	}

    private Map<String, Object> getTimeSeriesContextAttributes(ExecutionContext executionContext) {
        Map<String, Object> attributes = new HashMap<>();
        if (context.getPlan() != null) {
            attributes.put("planId", context.getPlan().getId().toString());
        }
        attributes.put("taskId", Objects.requireNonNullElse(context.get("$schedulerTaskId"), ""));

        TreeMap<String, String> additionalAttributes = (TreeMap<String, String>) executionContext.get(CTX_ADDITIONAL_ATTRIBUTES);
        if (additionalAttributes != null) {
            attributes.putAll(additionalAttributes);
        }

        return attributes;
    }

	 /**
	  * Return the children artefacts grouped by parent source
	  * By default, Artefacts are grouped by before, main and after source
	  * Handlers of artefacts defining additional {@link step.core.artefacts.ChildrenBlock} should override the method
	  * resolveChildrenArtefactBySource_ and return the map in the execution order
	  *
	  * @param artefactNode    : the artefact to resolve
	  * @return the children artefacts grouped by parent source
	  */
	public List<ResolvedChildren> resolveChildrenArtefactBySource(ARTEFACT artefactNode, String currentPath) {
		List<ResolvedChildren> results = new ArrayList<>();
		ChildrenBlock before = artefactNode.getBefore();
		if (before != null) {
			results.add(new ResolvedChildren(ParentSource.BEFORE, before.getSteps(), currentPath));
		}
		List<ResolvedChildren> handlerSpecificChildren = resolveChildrenArtefactBySource_(artefactNode, currentPath);
		results.addAll(handlerSpecificChildren);
		ChildrenBlock after = artefactNode.getAfter();
		if (after != null) {
			results.add(new ResolvedChildren(ParentSource.AFTER, after.getSteps(), currentPath));
		}
		return results;
	}

	protected List<ResolvedChildren> resolveChildrenArtefactBySource_(ARTEFACT artefactNode, String currentPath) {
		List<ResolvedChildren> results = new ArrayList<>();
		results.add(new ResolvedChildren(ParentSource.MAIN, artefactNode.getChildren(), currentPath));
		return results;
	}

	private boolean filterArtefact(ARTEFACT artefact) {
		ArtefactFilter filter = context.getExecutionParameters().getArtefactFilter();
		return (filter!=null&&!filter.isSelected(artefact)) || artefact.getSkipNode().get();
	}

	/**
	 * Execute the provided artefact and report the execution to the provided report node
	 * @param reportNode the {@link ReportNode} corresponding to the artefact
	 * @param artefact the {@link AbstractArtefact} to be executed
	 * @throws Exception
	 */
	protected abstract void execute_(REPORT_NODE reportNode, ARTEFACT artefact) throws Exception;
		
	@SuppressWarnings("unchecked")
	private REPORT_NODE beforeDelegation(Phase executionPhase, ReportNode parentReportNode, ARTEFACT artefact, Map<String, Object> newVariables, ParentSource parentSource) {
		REPORT_NODE reportNode;
		
		if(executionPhase == Phase.EXECUTION && artefact.isCreateSkeleton()) {
			// search for the report node that has been created during skeleton phase
			reportNode = (REPORT_NODE) reportNodeAccessor.getReportNodeByParentIDAndArtefactID(parentReportNode.getId(), artefact.getId());
			if(reportNode == null) {
				// the report node created during the createSkeleton phase couldn't be found.
				// the reason might be that at least one report node in the path to the current report node hasn't been persisted
				// if one node gets persisted or not depends on the ArtefactType: AbstractArtefact.isCreateSkeleton()
				// It is therefore depending on the Plan if all the nodes of the path are persisted.
				// We use to throw an exception in that case but it seems to be a better option to just ignore this
				// and create the node again instead of throwing an error
				reportNode = createReportNode(parentReportNode, artefact, parentSource);
				//throw new RuntimeException("Unable to find report node during execution phase. "
				//		+ "The report node should have been created during skeleton creation phase as the artefact has createSkeleton flag enabled. AbstractArtefact="+testArtefact.toString()+ ". ParentNode:"+ parentNode.toString());
			}
		} else {
			reportNode = createReportNode(parentReportNode, artefact, parentSource);
		}

		String artefactHash = getArtefactHash(artefact);
		reportNode.setArtefactHash(artefactHash);
		//All plan nodes can not be resolved before executions (i.e. recursive call plans), thus we check and update
		//the resolved plan nodes when required
		// Resolved plans might be disabled
		if (resolvedPlanBuilder != null) {
			resolvedPlanBuilder.checkAndAddMissingResolvedPlanNode(artefactHash, artefact, parentReportNode, reportNodeCache, reportNode.getParentSource());
		}

		context.setCurrentReportNode(reportNode);
		reportNodeCache.put(reportNode);
		
		if(newVariables!=null) {
			for(Entry<String, Object> var:newVariables.entrySet()) {
				variablesManager.putVariable(reportNode, var.getKey(), var.getValue());
			}
		}
		
		handleAttachments(artefact, reportNode);
		
		variablesManager.putVariable(parentReportNode, "currentArtefact", artefact);
		variablesManager.putVariable(parentReportNode, "currentReport", reportNode);
		
		addCustomReportNodeAttributes(reportNode);
		
		if(executionPhase == Phase.EXECUTION) {
			addReportNodeUpdateListener(reportNode);
		}
		
		return reportNode;
	}

	private String getArtefactHash(ARTEFACT artefact) {
		String currentArtefactPath = currentArtefactPath();
		return ArtefactPathHelper.generateArtefactHash(currentArtefactPath, artefact);
	}

	protected String currentArtefactPath() {
		return (String) variablesManager.getVariable(ARTEFACT_PATH);
	}

	protected void pushArtefactPath(ReportNode node, ARTEFACT artefact) {
		String currentArtefactPath = currentArtefactPath();
		String newArtefactPath = ArtefactPathHelper.getPathOfArtefact(currentArtefactPath, artefact);
		context.getVariablesManager().putVariable(node, ARTEFACT_PATH, newArtefactPath);
	}

	protected void delegateCreateReportSkeleton(AbstractArtefact artefact, ReportNode parentNode) {
		artefactHandlerManager.createReportSkeleton(artefact, parentNode, null, ParentSource.MAIN);
	}
	
	protected void delegateCreateReportSkeleton(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables) {
		artefactHandlerManager.createReportSkeleton(artefact, parentNode, newVariables , ParentSource.MAIN);
	}

	protected ReportNode delegateExecute(AbstractArtefact artefact, ReportNode parentNode) {
		return artefactHandlerManager.execute(artefact, parentNode, null, ParentSource.MAIN);
	}
	
	protected ReportNode delegateExecute(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables) {
		return artefactHandlerManager.execute(artefact, parentNode, newVariables, ParentSource.MAIN);
	}
	
	private void addReportNodeUpdateListener(REPORT_NODE node) {
		context.getEventManager().addReportNodeEventListener(node, new ReportNodeEventListener() {
			@Override
			public void onUpdate() {
				saveReportNode(node);
			}
			@Override
			public void onDestroy() {}
		});
	}

	private void addCustomReportNodeAttributes(REPORT_NODE node) {
		for(Entry<String,String> entry:((Map<String,String>)reportNodeAttributesManager.getCustomAttributes()).entrySet()) {
			node.addCustomAttribute(entry.getKey(), entry.getValue());
		}
	}
	
	private void afterDelegation(REPORT_NODE reportNode, ReportNode parentReportNode, ARTEFACT artefact) {
		reportNodeCache.remove(reportNode);
		variablesManager.releaseVariables(reportNode.getId());

		context.setCurrentReportNode(parentReportNode);
		variablesManager.putVariable(parentReportNode, "report", reportNode);
		
		context.getEventManager().notifyReportNodeDestroyed(reportNode);
	}

	protected Map<String, Object> getBindings() {
		return ExecutionContextBindings.get(context);
	}

	private static final String SKELETON_NOT_INIT = "SKELETON_NOT_INIT";

	private ResourceManager resourceManager;
	
	@SuppressWarnings("unchecked")
	private HashSet<String> getListOfArtefactsNotInitialized() {
		Object o = context.get(SKELETON_NOT_INIT);
		HashSet<String> result;
		if(o == null) {
			result = new HashSet<String>();
			context.put(SKELETON_NOT_INIT, result);
		} else {
			result = (HashSet<String>) o;
		}
		return result;
	}


	private ReportNode saveReportNode(REPORT_NODE reportNode) {
		AbstractArtefact resolvedArtefact = reportNode.getResolvedArtefact();
		if(resolvedArtefact != null) {
			List<AbstractArtefact> children = resolvedArtefact.getChildren();
			// save the resolved artefact without children to save space
			resolvedArtefact.setChildren(null);
			try {
				return reportNodeAccessor.save(reportNode);
			} finally {
				resolvedArtefact.setChildren(children);
			}
		} else {
			return reportNodeAccessor.save(reportNode);
		}
	}

	/**
	 * Mark all errors of the branch starting from the provided reportNode as non-contributing.
	 * See {@link ReportNode#getContributingError()} for more details about error contribution
	 * @param reportNode the root node of the branch
	 */
	protected void removeErrorContributionsInReportBranch(ReportNode reportNode) {
		reportNodeAccessor.getChildren(reportNode.getId()).forEachRemaining(this::removeErrorContributionsInReportBranch);

		context.getExecutionCallbacks().onErrorContributionRemoval(context, reportNode);

		Boolean contributingError = reportNode.getContributingError();
		if(contributingError != null && contributingError) {
			reportNode.setContributingError(false);
			reportNodeAccessor.save(reportNode);
		}

	}

	/**
	 * Prune the branch of the report tree starting from the provided reportNode.
	 * This will remove the provided report node and all its children
	 * @param reportNode the report node to be pruned
	 */
	protected void pruneReportBranch(ReportNode reportNode) {
		ObjectId reportNodeId = reportNode.getId();
		reportNodeAccessor.getChildren(reportNodeId).forEachRemaining(this::pruneReportBranch);
		reportNodeAccessor.remove(reportNodeId);
		context.getExecutionCallbacks().onReportNodeRemoval(context, reportNode);
	}

	private REPORT_NODE createReportNode(ReportNode parentReportNode, ARTEFACT artefact, ParentSource parentSource) {
		REPORT_NODE node = createReportNode_(parentReportNode, artefact);
		node.setId(new ObjectId());
		node.setName(getReportNodeName(artefact));
		node.setParentID(parentReportNode.getId());
		String parentPath = parentReportNode.getPath();
		node.setPath(((parentPath != null) ? parentPath : "") + node.getId());
		node.setArtefactID(artefact.getId());
		node.setExecutionID(context.getExecutionId().toString());
		node.setStatus(ReportNodeStatus.NORUN);
		node.setParentSource(parentSource);
		return node;
	}

	private String getReportNodeNameDynamically(ARTEFACT artefact) {
		String name = null;
		if (artefact.isUseDynamicName()) {
			name = artefact.getDynamicName().get();
		} else {
			name = artefact.getAttribute(AbstractArtefact.NAME);
		}
		return name != null ? name : "Unnamed";
	}

	private String getReportNodeName(ARTEFACT artefact) {
		String name = artefact.getAttribute(AbstractArtefact.NAME);
		return name != null ? name : "Unnamed";
	}

	/**
	 * Creates the {@link ReportNode} corresponding to the provided artefact
	 * @param parentReportNode the parent {@link ReportNode}
	 * @param artefact the artefact to create the node for
	 * @return 
	 */
	protected abstract REPORT_NODE createReportNode_(ReportNode parentReportNode, ARTEFACT artefact);	
	
	public List<AbstractArtefact> getChildren(AbstractArtefact artefact) { 
		return getChildren(artefact, context);
	}
	
	public static List<AbstractArtefact> getChildren(AbstractArtefact artefact, ExecutionContext context) { 
		return getAllChildren(artefact, context);
	}

	public static List<AbstractArtefact> getChildrenCopy(List<AbstractArtefact> sourceChildren, ExecutionContext context) {
		DynamicBeanResolver dynamicBeanResolver = context.getDynamicBeanResolver();
		List<AbstractArtefact> result = new ArrayList<>();
		if(sourceChildren!=null) {
			for(AbstractArtefact child:sourceChildren) {
				result.add(dynamicBeanResolver.cloneDynamicValues(child));
			}
		}
		return result;
	}

	private static List<AbstractArtefact> getAllChildren(AbstractArtefact artefact, ExecutionContext context) {
		DynamicBeanResolver dynamicBeanResolver = context.getDynamicBeanResolver();
		List<AbstractArtefact> result = new ArrayList<>();
		List<AbstractArtefact> children = artefact.getChildren();
		if(children!=null) {
			for(AbstractArtefact child:children) {
				result.add(dynamicBeanResolver.cloneDynamicValues(child));
			}
		}
		return result;
	}

	protected <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name) {
		return workArtefactFactory.createWorkArtefact(artefactClass, parentArtefact, name, false);
	}
	
	protected <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren,boolean persistNode) {
		return workArtefactFactory.createWorkArtefact(artefactClass, parentArtefact, name, copyChildren, persistNode);
	}

	protected <T extends AbstractArtefact> T createWorkArtefact(Class<T> artefactClass, AbstractArtefact parentArtefact, String name, boolean copyChildren) {
		return workArtefactFactory.createWorkArtefact(artefactClass, parentArtefact, name, copyChildren);
	}

	private void handleAttachments(AbstractArtefact artefact, ReportNode report) {
		List<ObjectId> attachments = artefact.getAttachments();
		if(attachments!=null) {
			for(ObjectId attachmentId:attachments) {
				File file = resourceManager.getResourceFile(attachmentId.toString()).getResourceFile();
				variablesManager.putVariable(report, FILE_VARIABLE_PREFIX+file.getName(), file);
			}
		}
	}
	
	protected void fail(ReportNode node, String error) {
		node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
		node.setError(error, 0, true);
	}
	
	protected void failWithException(ReportNode result, Throwable e) {
		failWithException(result, e, true);
	}
	
	protected void failWithException(ReportNode result, Throwable e, boolean generateAttachment) {
		failWithException(result, null, e, generateAttachment);
	}
	
	protected void failWithException(ReportNode result, String errorMsg, Throwable e, boolean generateAttachment) {
		if(logger.isDebugEnabled()) {
			logger.debug("Error in node", e);
		}
		if(generateAttachment && !(e instanceof ValidationException)) {			
			reportNodeAttachmentManager.attach(e, result);
		}
		result.setError(errorMsg!=null?errorMsg+":"+e.getMessage():e.getMessage(), 0, true);	
		result.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
	}
	
	protected void releaseTokens() {
		FunctionGroupHandle handle = getFunctionGroupHandle();
		if (handle != null) {			
			try {
				handle.releaseTokens(context, false);
			} catch (Exception e) {
				logger.warn("Could not release tokens",e);
			}
		}
	}
	
	protected boolean isInSession() {
		boolean result=false;
		FunctionGroupHandle handle = getFunctionGroupHandle();
		if (handle != null) {
			return handle.isInSession(context);
		}
		return result;
	}

	private FunctionGroupHandle getFunctionGroupHandle() {
		return context.get(FunctionGroupHandle.class);
	}
}
