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
package step.plugins.functions.types.composite;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.artefacts.handlers.FunctionRouter;
import step.artefacts.handlers.LocalFunctionRouterImpl;
import step.attachments.FileResolver;
import step.client.accessors.RemoteFunctionAccessorImpl;
import step.client.accessors.RemotePlanAccessorImpl;
import step.client.credentials.ControllerCredentials;
import step.client.resources.RemoteResourceManager;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.plans.PlanAccessor;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.variables.VariableType;
import step.functions.accessor.FunctionAccessor;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.handler.AbstractFunctionHandler;
import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.client.GridClient;
import step.grid.client.MockedGridClientImpl;
import step.plugins.java.GeneralScriptFunctionType;
import step.plugins.selenium.SeleniumFunctionType;
import step.resources.ResourceManager;

public class ArtefactFunctionHandler extends JsonBasedFunctionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ArtefactFunctionHandler.class);
	
	// Key agreed upon to pass the artefact serving as root to the handler (via props)
	public static final String PLANID_KEY = "$planid";
	
	@Override
	protected Output<JsonObject> handle(Input<JsonObject> input) {
		
		ExecutionContext executionContext = (ExecutionContext) getTokenReservationSession().get(AbstractFunctionHandler.EXECUTION_CONTEXT_KEY);
		
		if(executionContext == null) {
			executionContext = ContextBuilder.createLocalExecutionContext();
			GridClient gridClient = new MockedGridClientImpl();

			ControllerCredentials credentials = new ControllerCredentials("http://localhost:8080", "admin", "init");

			FunctionAccessor functionAccessor = new RemoteFunctionAccessorImpl(credentials);
			//FunctionExecutionService functionExecutionService = new RemoteFunctionExecutionService(credentials);
			ResourceManager resourceManager = new RemoteResourceManager(credentials);
			
			FunctionTypeRegistryImpl functionTypeRegistryImpl = new FunctionTypeRegistryImpl(new FileResolver(resourceManager), gridClient);
			// TODO implement all this properly
			functionTypeRegistryImpl.registerFunctionType(new GeneralScriptFunctionType(new Configuration()));
			Configuration configuration = new Configuration();
			configuration.putProperty("plugins.selenium.libs.3.x", "../distribution/template-controller/ext/selenium/selenium-java-3.5.3");
			functionTypeRegistryImpl.registerFunctionType(new SeleniumFunctionType(configuration));
			
			FunctionExecutionService functionExecutionService;
			try {
				functionExecutionService = new FunctionExecutionServiceImpl(gridClient, functionTypeRegistryImpl, executionContext.getDynamicBeanResolver());
				executionContext.put(FunctionAccessor.class, functionAccessor);
				executionContext.put(FunctionExecutionService.class, functionExecutionService);
				executionContext.put(FunctionRouter.class, new LocalFunctionRouterImpl(functionExecutionService));
				
				PlanAccessor planAccessor = new RemotePlanAccessorImpl(credentials);
				executionContext.setPlanAccessor(planAccessor);
			} catch (Exception e) {
				logger.error("Error while setting up execution context for composite keyword execution", e);
			}
			
		}
		
		String planId = input.getProperties().get(PLANID_KEY);
		String parentReportId = input.getProperties().get(AbstractFunctionHandler.PARENTREPORTID_KEY);
		
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
		
		executionContext.getVariablesManager().putVariable(parentNode, "input", input.getPayload());
		OutputBuilder output = new OutputBuilder();
		executionContext.getVariablesManager().putVariable(parentNode, VariableType.IMMUTABLE, "output", output);
		
		try {
			ReportNode node = ArtefactHandler.delegateExecute(executionContext, artefact,parentNode);
			if(node.getStatus()== ReportNodeStatus.TECHNICAL_ERROR || node.getStatus()== ReportNodeStatus.FAILED) {
				Error error = new Error();
				error.setCode(0);
				error.setMsg("Error in composite keyword");
				error.setRoot(false);
				error.setType(node.getStatus().equals(ReportNodeStatus.FAILED)?ErrorType.BUSINESS:ErrorType.TECHNICAL);
				output.setError(error);
			}
			return output.build();
		} finally {
			executionContext.getVariablesManager().removeVariable(parentNode, "output");
			executionContext.setCurrentReportNode(previousCurrentNode);
		}
	}

}
