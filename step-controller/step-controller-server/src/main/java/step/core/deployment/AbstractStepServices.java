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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.access.User;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectFilter;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.scheduler.ExecutionScheduler;
import step.framework.server.AbstractServices;
import step.framework.server.Session;
import step.framework.server.access.AuthorizationManager;

public abstract class AbstractStepServices extends AbstractServices<User> {

	public static final String SESSION = "session";

	protected Configuration configuration;

	private ObjectHookRegistry objectHookRegistry;

	@Inject
	GlobalContext serverContext;

	public AbstractStepServices() {
		super();
	}
	
	@PostConstruct
	public void init() throws Exception {
		configuration = serverContext.getConfiguration();
		objectHookRegistry = serverContext.get(ObjectHookRegistry.class);
	}

	protected GlobalContext getContext() {
		return serverContext;
	}

	protected ExecutionScheduler getScheduler() {
		return serverContext.getScheduler();
	}
	
	protected ExecutionContext getExecutionRunnable(String executionID) {
		return getScheduler().getCurrentExecutions().stream().filter(e->e.getExecutionId().equals(executionID))
				.findFirst().orElse(null);
	}
	
	protected ObjectEnricher getObjectEnricher() {
		return objectHookRegistry.getObjectEnricher(getSession());
	}

	protected ObjectFilter getObjectFilter() {
		return objectHookRegistry.getObjectFilter(getSession());
	}

	protected ObjectPredicate getObjectPredicate(){
		return objectHookRegistry.getObjectPredicate(getSession());
	}

	protected AuthorizationManager<User, Session<User>> getAuthorizationManager(){
		return getContext().require(AuthorizationManager.class);
	}

	protected void checkRightsOnBehalfOf(String right, String userOnBehalfOf) {
		Session<User> session = getSession();
		try {
			if (!getAuthorizationManager().checkRightInContext(session, right, userOnBehalfOf)) {
				User user = session.getUser();
				throw new AuthorizationException("User " + (user == null ? "" : user.getUsername()) + " has no permission on '" + right + "' (on behalf of " + userOnBehalfOf + ")");
			}
		} catch (NotMemberOfProjectException ex){
			// if 'userOnBehalf' is not a member of the project, we want to return 'access denied' error
			throw new AuthorizationException(ex.getMessage());
		}
	}
}
