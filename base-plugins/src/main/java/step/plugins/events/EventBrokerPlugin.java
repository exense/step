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
package step.plugins.events;

import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class EventBrokerPlugin extends AbstractPlugin {
		
	private EventBroker eventBroker;
	
	@Override
	public void executionControllerStart(GlobalContext context)  throws Exception {
		String circuitBreakerProp = context.getConfiguration().getProperty("eventbroker.circuitBreakerThreshold", "5000"); 
		eventBroker = new EventBroker(Long.parseLong(circuitBreakerProp));
		context.put(EventBroker.class, eventBroker);
		context.getServiceRegistrationCallback().registerService(EventBrokerServices.class);
	}

	@Override
	public void executionStart(ExecutionContext context) {
		context.put(EventBroker.class, eventBroker);
		super.executionStart(context);
	}
}
