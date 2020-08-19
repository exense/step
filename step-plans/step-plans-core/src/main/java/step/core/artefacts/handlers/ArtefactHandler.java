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
package step.core.artefacts.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactFilter;
import step.core.artefacts.WorkArtefactFactory;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeStatus;
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

public abstract class ArtefactHandler<ARTEFACT extends AbstractArtefact, REPORT_NODE extends ReportNode> {
	
	protected static Logger logger = LoggerFactory.getLogger(ArtefactHandler.class);
	
	public static String FILE_VARIABLE_PREFIX = "file:";

	protected ExecutionContext context;
	private ArtefactHandlerManager artefactHandlerManager;
	private ReportNodeAttachmentManager reportNodeAttachmentManager;
	private ReportNodeAttributesManager reportNodeAttributesManager;
	private WorkArtefactFactory workArtefactFactory = new WorkArtefactFactory();
	
	private ReportNodeAccessor reportNodeAccessor;
	private VariablesManager variablesManager;
	private ReportNodeCache reportNodeCache;
	private DynamicBeanResolver dynamicBeanResolver;
		
	public ArtefactHandler() {
		super();		
	}
	
	public void init(ExecutionContext context) {
		this.context = context;
		artefactHandlerManager = context.getArtefactHandlerManager();
		reportNodeAccessor = context.getReportNodeAccessor();
		reportNodeCache = context.getReportNodeCache();
		variablesManager = context.getVariablesManager();
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(context);
		reportNodeAttributesManager = new ReportNodeAttributesManager(context);
		dynamicBeanResolver = context.getDynamicBeanResolver();
		resourceManager = context.get(ResourceManager.class);
	}
	
	private enum Phase {
		
		SKELETON_CREATION,
		
		EXECUTION;
	}
	
	public void createReportSkeleton(ReportNode parentReportNode, ARTEFACT artefact, Map<String, Object> newVariables) {		
		REPORT_NODE reportNode = beforeDelegation(Phase.SKELETON_CREATION, parentReportNode, artefact, newVariables);
		if(parentReportNode != null && parentReportNode.isOrphan()) {
			reportNode.setOrphan(true);
		} else {
			reportNode.setOrphan(!artefact.isCreateSkeleton());
		}

		try {
			dynamicBeanResolver.evaluate(artefact, getBindings());
			artefact.setNameDynamically();
			ArtefactFilter filter = context.getExecutionParameters().getArtefactFilter();
			if(filter!=null&&!filter.isSelected(artefact)) {
				reportNode.setStatus(ReportNodeStatus.SKIPPED);
			} else {
				createReportSkeleton_(reportNode, artefact);
			}
		} catch (Exception e) {
			getListOfArtefactsNotInitialized().add(artefact.getId().toString());
			failWithException(reportNode, e, false);
		}
		
		if(artefact.isCreateSkeleton() && !reportNode.isOrphan()) {
			saveReportNode(reportNode);
		}
		
		context.getExecutionCallbacks().afterReportNodeSkeletonCreation(context, reportNode);
		
		afterDelegation(reportNode, parentReportNode, artefact);
	}
	
	protected abstract void createReportSkeleton_(REPORT_NODE parentNode, ARTEFACT testArtefact);
	
	public ReportNode execute(REPORT_NODE parentReportNode, ARTEFACT artefact, Map<String, Object> newVariables) {		
		// If the artefact hasn't been initialized during createReportSkeleton phase, relaunch the skeleton creation phase for this node
		if(getListOfArtefactsNotInitialized().contains(artefact.getId().toString())) {
			createReportSkeleton(parentReportNode, artefact, newVariables);
		}
		
		REPORT_NODE reportNode = beforeDelegation(Phase.EXECUTION, parentReportNode, artefact, newVariables);
		
		long t1 = System.currentTimeMillis();
		reportNode.setExecutionTime(t1);
		reportNode.setStatus(ReportNodeStatus.RUNNING);
		
		boolean persistBefore = variablesManager.getVariableAsBoolean("tec.execution.reportnodes.persistbefore",true);		
		boolean persistAfter = variablesManager.getVariableAsBoolean("tec.execution.reportnodes.persistafter",true);
		boolean persistOnlyNonPassed = variablesManager.getVariableAsBoolean("tec.execution.reportnodes.persistonlynonpassed",false);
		
		try {
			context.getExecutionCallbacks().beforeReportNodeExecution(context, reportNode);
			
			dynamicBeanResolver.evaluate(artefact, getBindings());
			artefact.setNameDynamically();
			reportNode.setArtefactInstance(artefact);
			reportNode.setResolvedArtefact(artefact);
			
			ArtefactFilter filter = context.getExecutionParameters().getArtefactFilter();
			if((filter!=null&&!filter.isSelected(artefact)) || artefact.getSkipNode().get()) {
				reportNode.setStatus(ReportNodeStatus.SKIPPED);
			} else {
				if(persistBefore && artefact.isPersistNode()) {
					saveReportNode(reportNode);					
				}
				
				execute_(reportNode, artefact);
			}
		} catch (Exception e) {
			failWithException(reportNode, e);
		}
		long duration = System.currentTimeMillis() - t1;
		
		reportNode.setDuration((int)duration);

		if(persistAfter && artefact.isPersistNode()) {
			if(!persistOnlyNonPassed){
				saveReportNode(reportNode);
			} else {
				if(!reportNode.getStatus().equals(ReportNodeStatus.PASSED)){
					saveReportNode(reportNode);
				}
			}
		}
		
		context.getExecutionCallbacks().afterReportNodeExecution(context, reportNode);
		
		afterDelegation(reportNode, parentReportNode, artefact);
		
		return reportNode;
	}
	
	/**
	 * Execute the provided artefact and report the execution to the provided report node
	 * @param reportNode the {@link ReportNode} corresponding to the artefact
	 * @param artefact the {@link AbstractArtefact} to be executed
	 * @throws Exception
	 */
	protected abstract void execute_(REPORT_NODE reportNode, ARTEFACT artefact) throws Exception;
		
	@SuppressWarnings("unchecked")
	private REPORT_NODE beforeDelegation(Phase executionPhase, ReportNode parentReportNode, ARTEFACT artefact, Map<String, Object> newVariables) {
		REPORT_NODE reportNode;
		
		if(executionPhase==Phase.EXECUTION && artefact.isCreateSkeleton()) {
			// search for the report node that has been created during skeleton phase
			reportNode = (REPORT_NODE) reportNodeAccessor.getReportNodeByParentIDAndArtefactID(parentReportNode.getId(), artefact.getId());
			if(reportNode == null) {
				// the report node created during the createSkeleton phase couldn't be found.
				// the reason might be that at least one report node in the path to the current report node hasn't been persisted
				// if one node gets persisted or not depends on the ArtefactType: AbstractArtefact.isCreateSkeleton()
				// It is therefore depending on the Plan if all the nodes of the path are persisted.
				// We use to throw an exception in that case but it seems to be a better option to just ignore this
				// and create the node again instead of throwing an error
				reportNode = createReportNode(parentReportNode, artefact);
				//throw new RuntimeException("Unable to find report node during execution phase. "
				//		+ "The report node should have been created during skeleton creation phase as the artefact has createSkeleton flag enabled. AbstractArtefact="+testArtefact.toString()+ ". ParentNode:"+ parentNode.toString());
			}
		} else {
			reportNode = createReportNode(parentReportNode, artefact);			
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
		
		if(executionPhase==Phase.EXECUTION) {
			addReportNodeUpdateListener(reportNode);
		}
		
		return reportNode;
	}

	protected void delegateCreateReportSkeleton(AbstractArtefact artefact, ReportNode parentNode) {
		artefactHandlerManager.createReportSkeleton(artefact, parentNode, null);
	}
	
	protected void delegateCreateReportSkeleton(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables) {
		artefactHandlerManager.createReportSkeleton(artefact, parentNode, newVariables);
	}

	protected ReportNode delegateExecute(AbstractArtefact artefact, ReportNode parentNode) {
		return artefactHandlerManager.execute(artefact, parentNode, null);
	}
	
	protected ReportNode delegateExecute(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables) {
		return artefactHandlerManager.execute(artefact, parentNode, newVariables);
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
		variablesManager.releaseVariables(reportNode.getId().toString());

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
	
	protected void removeReportNode(ReportNode node) {
		reportNodeAccessor.getChildren(node.getId()).forEachRemaining(e->removeReportNode(e));
		reportNodeAccessor.remove(node.getId());
		context.getExecutionCallbacks().rollbackReportNode(context, node);
	}

	private REPORT_NODE createReportNode(ReportNode parentReportNode, ARTEFACT artefact) {
		REPORT_NODE node = createReportNode_(parentReportNode, artefact);
		node.setId(new ObjectId());
		node.setName(getReportNodeName(artefact));
		node.setParentID(parentReportNode.getId());
		node.setArtefactID(artefact.getId());
		node.setExecutionID(context.getExecutionId().toString());
		node.setStatus(ReportNodeStatus.NORUN);
		return node;
	}

	private String getReportNodeName(ARTEFACT artefact) {
		Map<String, String> attributes = artefact.getAttributes();
		if (attributes != null) {
			String name = attributes.get(AbstractArtefact.NAME);
			if (name != null) {
				return name;
			}
		}
		return "Unnamed";
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
