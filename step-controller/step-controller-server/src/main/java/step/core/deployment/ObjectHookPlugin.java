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

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectFilter;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.ql.Filter;
import step.core.ql.OQLFilterBuilder;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
@IgnoreDuringAutoDiscovery
public class ObjectHookPlugin extends AbstractExecutionEnginePlugin {
	
	private final ObjectHookRegistry objectHookRegistry;
	
	public ObjectHookPlugin(ObjectHookRegistry objectHookRegistry) {
		super();
		this.objectHookRegistry = objectHookRegistry;
	}

	@Override
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
			ExecutionContext executionContext) {
		// Rebuild the session based on the ExecutionParameters of the execution context
		// This has to be done because the Session is not always available when running an execution
		// (by Scheduled tasks for instance) 
		Session session = new Session();
		objectHookRegistry.forEach(hook->{
			try {
				hook.rebuildContext(session, executionContext.getExecutionParameters());
			} catch (Exception e) {
				ObjectHookControllerPlugin.logger.error("Error while rebuilding context for execution "+executionContext.getExecutionId(), e);
			}
		});
		
		// Add the composed object enricher and filters to the execution context
		executionContext.put(ObjectEnricher.class, objectHookRegistry.getObjectEnricher(session));
		ObjectFilter objectFilter = objectHookRegistry.getObjectFilter(session);
		executionContext.put(ObjectFilter.class, objectFilter);
		
		Filter<Object> filter = OQLFilterBuilder.getFilter(objectFilter.getOQLFilter());
		executionContext.put(ObjectPredicate.class, new ObjectPredicate() {
			@Override
			public boolean test(Object t) {
				return filter.test(t);
			}
		});
	}
}
