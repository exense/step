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
package step.core.deployment;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import ch.exense.commons.app.Configuration;
import step.core.Controller;
import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.scheduler.ExecutionScheduler;

public abstract class AbstractServices {

	private static final String SESSION = "session";

	@Inject
	protected Controller controller;
	
	@Inject 
	private HttpSession httpSession;
	
	protected Configuration configuration;

	public AbstractServices() {
		super();
	}
	
	@PostConstruct
	public void init() throws Exception {
		configuration = controller.getContext().getConfiguration();
	}

	protected GlobalContext getContext() {
		return controller.getContext();
	}

	protected ExecutionScheduler getScheduler() {
		return controller.getScheduler();
	}
	
	protected ExecutionContext getExecutionRunnable(String executionID) {
		return getScheduler().getCurrentExecutions().stream().filter(e->e.getExecutionId().equals(executionID))
				.findFirst().orElse(null);
	}
	
	protected Session getSession() {
		if(httpSession != null) {
			return (Session) httpSession.getAttribute(SESSION);
		} else {
			return null;
		}
	}
	
	protected void setSession(Session session) {
		httpSession.setAttribute(SESSION, session);
	}
}
