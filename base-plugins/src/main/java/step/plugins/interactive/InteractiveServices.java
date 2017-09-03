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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import step.artefacts.CallFunction;
import step.artefacts.FunctionGroup;
import step.artefacts.handlers.FunctionGroupHandler;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.commons.conf.Configuration;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.plans.LocalPlanRepository;
import step.core.plans.Plan;
import step.core.variables.VariableType;
import step.functions.Function;
import step.functions.FunctionExecutionService;
import step.functions.FunctionRepository;
import step.grid.TokenWrapper;
import step.grid.client.GridClient.AgentCommunicationException;
import step.planbuilder.FunctionPlanBuilder;
import step.planbuilder.PlanBuilder;
import step.plugins.parametermanager.ParameterManager;
import step.plugins.parametermanager.ParameterManagerPlugin;

@Singleton
@Path("interactive")
public class InteractiveServices extends AbstractServices {

	private Map<String, InteractiveSession> sessions = new ConcurrentHashMap<>();
	
	private Timer sessionExpirationTimer; 
	
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
				final int sessionTimeout = Configuration.getInstance().getPropertyAsInteger("ui.artefacteditor.interactive.sessiontimeout.minutes", 10)*60000;
				long time = System.currentTimeMillis();
				
				sessions.forEach((sessionId,session)->{
					if((session.lasttouch+sessionTimeout)<time) {
						try {
							closeSession(session);
						} catch (AgentCommunicationException e) {
							
						}
						sessions.remove(sessionId);
					}
				});
			}
		}, 60000, 60000);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/start")
	@Secured(right="interactive")
	public String start() throws AgentCommunicationException {
		InteractiveSession session = new InteractiveSession();
		ExecutionContext  executionContext = ContextBuilder.createContext(getContext());;
		session.c = executionContext;
		session.lasttouch = System.currentTimeMillis();
		session.root = new ReportNode();
		session.functionGroupContext = new FunctionGroupContext(null);
		String id = executionContext.getExecutionId();
		
		executionContext.getReportNodeCache().put(session.root);
		executionContext.setReport(session.root);
		ExecutionContext.setCurrentReportNode(session.root);
		session.c.getVariablesManager().putVariable(session.root, FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY, 
				session.functionGroupContext);

		sessions.put(id, session);
		return id;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/stop")
	@Secured(right="interactive")
	public void stop(@PathParam("id") String sessionId) throws AgentCommunicationException {
		InteractiveSession session = getAndTouchSession(sessionId);
		if(session!=null) {
			closeSession(session);			
		}
	}

	private void closeSession(InteractiveSession session) throws AgentCommunicationException {
		TokenWrapper token = session.functionGroupContext.getToken();
		if(token!=null) {
			FunctionExecutionService functionExecutionService = getContext().get(FunctionExecutionService.class);
			functionExecutionService.returnTokenHandle(token);
		}
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
	@Path("/{id}/execute/{artefactid}")
	@Secured(right="interactive")
	public ReportNode executeArtefact(@PathParam("id") String sessionId, @PathParam("artefactid") String artefactId, ExecutionParameters executionParameters) {
		InteractiveSession session = getAndTouchSession(sessionId);
		if(session!=null) {
			ArtefactAccessor a = getContext().getArtefactAccessor();
			AbstractArtefact artefact = a.get(artefactId);

			session.c.getArtefactCache().clear();

			ParameterManager parameterManager = (ParameterManager) getContext().get(ParameterManagerPlugin.KEY);
			ExecutionContext.setCurrentReportNode(session.root);
			ParameterManagerPlugin.putVariables(session.c, session.root, executionParameters.getExecutionParameters(), VariableType.IMMUTABLE);
			Map<String, String> parameters = parameterManager.getAllParameters(ExecutionContextBindings.get(session.c));
			ParameterManagerPlugin.putVariables(session.c, session.root, parameters, VariableType.IMMUTABLE);	
			
			ArtefactHandler.delegateCreateReportSkeleton(session.c, artefact, session.root);
			ArtefactHandler.delegateExecute(session.c, artefact, session.root);

			return null;			
		} else {
			 throw new RuntimeException("Session doesn't exist or expired.");
		}
		
	}
	
	public static class FunctionTestingSession {
		
		private String rootArtefactId;
		private String callFunctionId;
		
		public FunctionTestingSession() {
			super();
		}

		public String getRootArtefactId() {
			return rootArtefactId;
		}

		public void setRootArtefactId(String rootArtefactId) {
			this.rootArtefactId = rootArtefactId;
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
		
		CallFunction callFunction = FunctionPlanBuilder.keywordById(keywordid,"{}");
		
		// TODO do this centrally. Currently the same logic is implemented in the UI
		FunctionRepository functionRepository = getContext().get(FunctionRepository.class);
		Function function = functionRepository.getFunctionById(keywordid);
		Map<String, String> attributes = new HashMap<>();
		attributes.put("name", function.getAttributes().get(Function.NAME));
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
		LocalPlanRepository repo = new LocalPlanRepository(getContext().getArtefactAccessor());
		repo.save(plan);
		FunctionTestingSession result = new FunctionTestingSession();
		result.setRootArtefactId(plan.getRoot().getId().toString());
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
