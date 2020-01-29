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
package step.plugins.interactive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.CallFunction;
import step.artefacts.FunctionGroup;
import step.artefacts.handlers.FunctionGroupHandler;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.core.GlobalContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.execution.ControllerExecutionContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;
import step.core.plans.PlanNavigator;
import step.core.plans.builder.PlanBuilder;
import step.core.variables.VariableType;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.manager.FunctionManager;
import step.grid.TokenWrapper;
import step.grid.client.AbstractGridClientImpl.AgentCommunicationException;
import step.planbuilder.FunctionArtefacts;
import step.plugins.parametermanager.ParameterManagerPlugin;

@Singleton
@Path("interactive")
public class InteractiveServices extends AbstractServices {

	private static final Logger logger = LoggerFactory.getLogger(InteractiveServices.class);
	
	private Map<String, InteractiveSession> sessions = new ConcurrentHashMap<>();
	
	private Timer sessionExpirationTimer; 
	
	private ObjectHookRegistry objectHookRegistry;
	
	private ControllerExecutionContextBuilder executionContextBuilder;
	
	private static class InteractiveSession {
		
		ExecutionContext c;
		
		ReportNode root;
		
		FunctionGroupContext functionGroupContext;
		
		long lasttouch;
	}
	
	
	public InteractiveServices() {
		super();
		
		sessionExpirationTimer = new Timer("Session expiration timer");
		sessionExpirationTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				final int sessionTimeout = configuration.getPropertyAsInteger("ui.artefacteditor.interactive.sessiontimeout.minutes", 10)*60000;
				long time = System.currentTimeMillis();
				
				sessions.forEach((sessionId,session)->{
					if((session.lasttouch+sessionTimeout)<time) {
						try {
							closeSession(session);
						} catch (FunctionExecutionServiceException e) {
							
						}
						sessions.remove(sessionId);
					}
				});
			}
		}, 60000, 60000);
	}
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		objectHookRegistry = context.get(ObjectHookRegistry.class);
		executionContextBuilder = new ControllerExecutionContextBuilder(context);
	}
	
	@PreDestroy
	private void close() {
		if(sessionExpirationTimer != null) {
			sessionExpirationTimer.cancel();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/start")
	@Secured(right="interactive")
	public String start() throws AgentCommunicationException {
		InteractiveSession session = new InteractiveSession();
		ExecutionContext  executionContext = executionContextBuilder.createExecutionContext();
		
		// Enrich the ExecutionParameters with the current context attributes as done by the TenantContextFilter when starting a normal execution
		objectHookRegistry.getObjectEnricher(getSession()).accept(executionContext.getExecutionParameters());
		
		session.c = executionContext;
		session.lasttouch = System.currentTimeMillis();
		session.root = new ReportNode();
		session.functionGroupContext = new FunctionGroupContext(null);
		String id = executionContext.getExecutionId();
		
		executionContext.getReportNodeCache().put(session.root);
		executionContext.setReport(session.root);
		executionContext.setCurrentReportNode(session.root);
		session.c.getVariablesManager().putVariable(session.root, FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY, 
				session.functionGroupContext);

		executionContext.getExecutionCallbacks().executionStart(executionContext);
		sessions.put(id, session);
		return id;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/stop")
	@Secured(right="interactive")
	public void stop(@PathParam("id") String sessionId) throws FunctionExecutionServiceException {
		InteractiveSession session = getAndTouchSession(sessionId);
		if(session!=null) {
			closeSession(session);			
		}
	}

	private void closeSession(InteractiveSession session) throws FunctionExecutionServiceException {
		List<TokenWrapper> tokens = session.functionGroupContext.getTokens();
		if(tokens!=null) {
			FunctionExecutionService functionExecutionService = getContext().get(FunctionExecutionService.class);
			tokens.forEach(t->{
				try {
					functionExecutionService.returnTokenHandle(t.getID());
				} catch (FunctionExecutionServiceException e) {
					logger.warn("Error while closing interactive session", e);
				}
			});
		}
		session.c.getExecutionCallbacks().afterExecutionEnd(session.c);
	}
	
	public static class ExecutionParameters {
		
		Map<String, String> executionParameters;

		public ExecutionParameters() {
			super();
		}

		public Map<String, String> getExecutionParameters() {
			return executionParameters;
		}

		public void setExecutionParameters(Map<String, String> executionParameters) {
			this.executionParameters = executionParameters;
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/execute/{planid}/{artefactid}")
	@Secured(right="interactive")
	public ReportNode executeArtefact(@PathParam("id") String sessionId, @PathParam("planid") String planId, @PathParam("artefactid") String artefactId, ExecutionParameters executionParameters, @Context ContainerRequestContext crc) {
		InteractiveSession session = getAndTouchSession(sessionId);
		if(session!=null) {
			Plan plan = session.c.getPlanAccessor().get(planId);
			AbstractArtefact artefact = new PlanNavigator(plan).findArtefactById(artefactId);

			session.c.setCurrentReportNode(session.root);
			ParameterManagerPlugin.putVariables(session.c, session.root, executionParameters.getExecutionParameters(), VariableType.IMMUTABLE);
			
			ArtefactHandler.delegateCreateReportSkeleton(session.c, artefact, session.root);
			ArtefactHandler.delegateExecute(session.c, artefact, session.root);

			return null;			
		} else {
			 throw new RuntimeException("Session doesn't exist or expired.");
		}
		
	}
	
	public static class FunctionTestingSession {
		
		private String planId;
		private String callFunctionId;
		
		public FunctionTestingSession() {
			super();
		}

		public String getPlanId() {
			return planId;
		}

		public void setPlanId(String planId) {
			this.planId = planId;
		}

		public String getCallFunctionId() {
			return callFunctionId;
		}

		public void setCallFunctionId(String callFunctionId) {
			this.callFunctionId = callFunctionId;
		}
		
	}
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/functiontest/{keywordid}/start")
	@Secured(right="interactive")
	public FunctionTestingSession startFunctionTestingSession(@PathParam("keywordid") String keywordid) throws AgentCommunicationException {
		
		CallFunction callFunction = FunctionArtefacts.keywordById(keywordid,"{}");
		
		// TODO do this centrally. Currently the same logic is implemented in the UI
		FunctionManager functionManager = getContext().get(FunctionManager.class);
		Function function = functionManager.getFunctionById(keywordid);
		Map<String, String> attributes = new HashMap<>();
		attributes.put("name", function.getAttributes().get(AbstractOrganizableObject.NAME));
		callFunction.setAttributes(attributes);
		FunctionGroup functionGroup = new FunctionGroup();
		attributes = new HashMap<>();
		attributes.put("name", "Session");
		functionGroup.setAttributes(attributes);
		
		Plan plan = PlanBuilder.create()
				.startBlock(functionGroup)
				.add(callFunction)
				.endBlock()
				.build();
		getContext().getPlanAccessor().save(plan);
		FunctionTestingSession result = new FunctionTestingSession();
		result.setPlanId(plan.getId().toString());
		result.setCallFunctionId(callFunction.getId().toString());
		return result;
	}

	private InteractiveSession getAndTouchSession(String sessionId) {
		InteractiveSession session = sessions.get(sessionId);
		if(session!=null) {
			session.lasttouch = System.currentTimeMillis();			
		}
		return session;
	}
}
