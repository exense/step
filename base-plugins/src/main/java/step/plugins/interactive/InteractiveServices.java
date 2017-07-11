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

import step.artefacts.handlers.FunctionGroupHandler;
import step.commons.conf.Configuration;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.execution.ContextBuilder;
import step.core.execution.ExecutionContext;
import step.functions.FunctionExecutionService;
import step.grid.TokenWrapper;
import step.grid.client.GridClient.AgentCommunicationException;

@Singleton
@Path("interactive")
public class InteractiveServices extends AbstractServices {

	private Map<String, InteractiveSession> sessions = new ConcurrentHashMap<>();
	
	private Timer sessionExpirationTimer; 
	
	private static class InteractiveSession {
		
		ExecutionContext c;
		
		ReportNode root;
		
		TokenWrapper wrapper;
		
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
		String id = executionContext.getExecutionId();
		
		FunctionExecutionService functionExecutionService = getContext().get(FunctionExecutionService.class);
		TokenWrapper wrapper = functionExecutionService.getTokenHandle(null, null, true);
		session.wrapper = wrapper;
		
		executionContext.getReportNodeCache().put(session.root);
		executionContext.setReport(session.root);
		ExecutionContext.setCurrentReportNode(session.root);
		session.c.getVariablesManager().putVariable(session.root, FunctionGroupHandler.TOKEN_PARAM_KEY, wrapper);
		
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
		FunctionExecutionService functionExecutionService = getContext().get(FunctionExecutionService.class);
		functionExecutionService.returnTokenHandle(session.wrapper);
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}/execute/{artefactid}")
	@Secured(right="interactive")
	public ReportNode executeArtefact(@PathParam("id") String sessionId, @PathParam("artefactid") String artefactId) {
		InteractiveSession session = getAndTouchSession(sessionId);
		if(session!=null) {
			ArtefactAccessor a = getContext().getArtefactAccessor();
			AbstractArtefact artefact = a.get(artefactId);
			
			ReportNode report = new ReportNode();
			
			ArtefactHandler.delegateCreateReportSkeleton(session.c, artefact, session.root);
			ArtefactHandler.delegateExecute(session.c, artefact, session.root);
			
			return report;			
		} else {
			 throw new RuntimeException("Session doesn't exist or expired.");
		}
		
	}

	private InteractiveSession getAndTouchSession(String sessionId) {
		InteractiveSession session = sessions.get(sessionId);
		if(session!=null) {
			session.lasttouch = System.currentTimeMillis();			
		}
		return session;
	}
}
