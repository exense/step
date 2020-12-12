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
package step.plugins.functions.types.composite;

import java.io.IOException;
import java.util.Map;

import javax.json.JsonObject;

import org.bson.types.ObjectId;

import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.artefacts.handlers.FunctionRouter;
import step.attachments.FileResolver;
import step.client.accessors.RemoteFunctionAccessorImpl;
import step.client.accessors.RemotePlanAccessorImpl;
import step.client.accessors.RemoteReportNodeAccessorImpl;
import step.client.credentials.ControllerCredentials;
import step.client.resources.RemoteResourceManager;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.BufferedReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.variables.VariableType;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.handler.AbstractFunctionHandler;
import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.functions.manager.FunctionManager;
import step.functions.manager.FunctionManagerImpl;
import step.functions.type.FunctionTypeRegistry;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.client.GridClient;
import step.planbuilder.FunctionArtefacts;
import step.resources.ResourceManager;

public class ArtefactFunctionHandler extends JsonBasedFunctionHandler {

	private static final String INPUT = "input";
	private static final String OUTPUT = "output";

	// Key agreed upon to pass the artefact serving as root to the handler (via props)
	public static final String PLANID_KEY = "$planid";
	
	@Override
	protected Output<JsonObject> handle(Input<JsonObject> input) throws IOException {
		OutputBuilder output = new OutputBuilder();

		
		String parentReportId = input.getProperties().get(AbstractFunctionHandler.PARENTREPORTID_KEY);
		
		ExecutionContext executionContext = (ExecutionContext) getTokenReservationSession().get(AbstractFunctionHandler.EXECUTION_CONTEXT_KEY);
		
		if(executionContext == null) {
			ControllerCredentials credentials = new ControllerCredentials("http://localhost:8080", "admin", "init");
			
			ReportNode parentReportNode;
			try(RemoteReportNodeAccessorImpl remoteReportNodeAccessor = new RemoteReportNodeAccessorImpl(credentials)) {
				parentReportNode = remoteReportNodeAccessor.get(parentReportId);
			}

			GridClient gricClient = new LocalGridClientImpl(getApplicationContextBuilder());
			try(RemoteExecutionContext remoteExecutionContext = new RemoteExecutionContext(credentials, gricClient, parentReportNode.getExecutionID(), parentReportId)) {
				RemoteFunctionPlugin plugin = new RemoteFunctionPlugin(remoteExecutionContext);
				PlanAccessor planAccessor = remoteExecutionContext.getPlanAccessor();
				
				ExecutionEngine engine = ExecutionEngine.builder().withParentContext(remoteExecutionContext).withPluginsFromClasspath().withOperationMode(OperationMode.CONTROLLER)
						.withPlugin(plugin).build();
				
				String planId = input.getProperties().get(PLANID_KEY);
				Plan plan = planAccessor.get(planId);
				Plan session = PlanBuilder.create().startBlock(FunctionArtefacts.session()).add(plan.getRoot()).endBlock().build();
				PlanRunnerResult result = engine.execute(session);
				
				// TODO send measurements to controller
				//output.setError("Running composite Keyword on agent not supported. Please change the keyword configuration accordingly.");
			};
		} else {
			String planId = input.getProperties().get(PLANID_KEY);
			
			ReportNode parentNode;
			if(parentReportId!=null) {
				parentNode = executionContext.getReportNodeAccessor().get(parentReportId);
				if(parentNode == null) {
					parentNode = executionContext.getCurrentReportNode();
				}
			} else {
				parentNode = executionContext.getCurrentReportNode();
				//throw new RuntimeException("Parent node id is null. This should never occur");
			}
			
			ReportNode previousCurrentNode = executionContext.getCurrentReportNode();
			executionContext.setCurrentReportNode(parentNode);
			executionContext.getReportNodeCache().put(parentNode);
			
			AbstractArtefact artefact = executionContext.getPlanAccessor().get(planId).getRoot();
			
			executionContext.getVariablesManager().putVariable(parentNode, INPUT, input.getPayload());
			executionContext.getVariablesManager().putVariable(parentNode, VariableType.IMMUTABLE, OUTPUT, output);
			
			try {
				ReportNode node = executionContext.getArtefactHandlerManager().execute(artefact, parentNode);
				if(node.getStatus()== ReportNodeStatus.TECHNICAL_ERROR || node.getStatus()== ReportNodeStatus.FAILED) {
					Error error = new Error();
					error.setCode(0);
					error.setMsg("Error in composite keyword");
					error.setRoot(false);
					error.setType(node.getStatus().equals(ReportNodeStatus.FAILED)?ErrorType.BUSINESS:ErrorType.TECHNICAL);
					output.setError(error);
				}
			} finally {
				executionContext.getVariablesManager().removeVariable(parentNode, OUTPUT);
				executionContext.setCurrentReportNode(previousCurrentNode);
			}
		}
		
		return output.build();
	}
	
	public static class RemoteExecutionContext extends AbstractExecutionEngineContext implements AutoCloseable {
		
		private final RemotePlanAccessorImpl planAccessor;
		private final RemoteResourceManager resourceManager;
		private final FileResolver fileResolver;
		private final BufferedReportNodeAccessor reportNodeAccessor;
		private final RemoteFunctionAccessorImpl functionAccessor;

		public RemoteExecutionContext(ControllerCredentials credentials, GridClient gridClient, String executionId, String rootReportNodeParentId) {
			super();
			
			planAccessor = new RemotePlanAccessorImpl(credentials);
			resourceManager = new RemoteResourceManager(credentials);
			fileResolver = new FileResolver(resourceManager);
			reportNodeAccessor = new BufferedReportNodeAccessor(new RemoteReportNodeAccessorImpl(credentials), node->{
				if(node.getParentID() == null) {
					node.setParentID(new ObjectId(rootReportNodeParentId));
				}
				node.setExecutionID(executionId);
			});
			functionAccessor = new RemoteFunctionAccessorImpl(credentials);
			
			put(GridClient.class, gridClient);
		}
		
		public PlanAccessor getPlanAccessor() {
			return planAccessor;
		}

		public ResourceManager getResourceManager() {
			return resourceManager;
		}

		public FileResolver getFileResolver() {
			return fileResolver;
		}

		public ReportNodeAccessor getReportNodeAccessor() {
			return reportNodeAccessor;
		}

		public FunctionAccessor getFunctionAccessor() {
			return functionAccessor;
		}

		@Override
		public void close() {
			try {
				reportNodeAccessor.close();
				functionAccessor.close();
				resourceManager.close();
				planAccessor.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	@IgnoreDuringAutoDiscovery
	@Plugin(dependencies = FunctionPlugin.class)
	public static class RemoteFunctionPlugin extends AbstractExecutionEnginePlugin {
		
		private final RemoteExecutionContext remoteExecutionContext;

		public RemoteFunctionPlugin(RemoteExecutionContext remoteExecutionContext) {
			super();
			this.remoteExecutionContext = remoteExecutionContext;
		}

		@Override
		public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
			GridClient gridClient = executionEngineContext.require(GridClient.class);
			
			TokenWrapper localTokenHandle = gridClient.getLocalTokenHandle();
			
			FunctionTypeRegistry functionTypeRegistry = executionContext.require(FunctionTypeRegistry.class);
			FunctionAccessor functionAccessor = remoteExecutionContext.getFunctionAccessor();
			FunctionManagerImpl functionManager = new FunctionManagerImpl(functionAccessor, functionTypeRegistry);
			
			executionContext.put(FunctionAccessor.class, functionAccessor);
			executionContext.put(FunctionManager.class, functionManager);
			
			FunctionExecutionService functionExecutionService;
			try {
				functionExecutionService = new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, executionContext.getDynamicBeanResolver());
			} catch (FunctionExecutionServiceException e) {
				throw new RuntimeException(e);
			}
			executionContext.put(FunctionExecutionService.class, functionExecutionService);
			executionContext.put(FunctionRouter.class, new FunctionRouter() {
				@Override
				public TokenWrapper selectToken(CallFunction callFunction, Function function, FunctionGroupContext functionGroupContext,
						Map<String, Object> bindings, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
					return localTokenHandle;
				}
			});
		}
		
	}
}
