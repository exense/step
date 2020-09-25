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
package step.functions.execution;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import junit.framework.Assert;
import step.grid.TokenWrapper;
import step.grid.client.TokenLifecycleStrategy;
import step.grid.client.TokenLifecycleStrategyCallback;
import step.grid.io.AgentError;
import step.grid.io.AgentErrorCode;
import step.grid.io.OutputMessage;

public class ConfigurableTokenLifecycleStrategyTest {

	@Test
	public void testAddErrorOnTokenCallErrorTrue() {
		AtomicBoolean callBackCalled = new AtomicBoolean(false);
		TokenLifecycleStrategy s = new ConfigurableTokenLifecycleStrategy(true, true, true, true, null);
		s.afterTokenCallError(new TokenLifecycleStrategyCallback(null, null) {

			@Override
			public void addTokenError(String errorMessage, Exception exception) {
				Assert.assertEquals("my message", exception.getMessage());
				callBackCalled.set(true);
			}
			
		}, new TokenWrapper(), new Exception("my message"));

		Assert.assertTrue(callBackCalled.get());
	}
	
	@Test
	public void testAddErrorOnTokenCallErrorFalse() {
		AtomicBoolean callBackCalled = new AtomicBoolean(false);
		TokenLifecycleStrategy s = new ConfigurableTokenLifecycleStrategy(true, true, false, true, null);
		s.afterTokenCallError(new TokenLifecycleStrategyCallback(null, null) {

			@Override
			public void addTokenError(String errorMessage, Exception exception) {
				Assert.assertEquals("my message", exception.getMessage());
				callBackCalled.set(true);
			}
			
		}, new TokenWrapper(), new Exception("my message"));

		Assert.assertFalse(callBackCalled.get());
	}
	
	@Test
	public void testAddErrorOnAgentErrorFalse() {
		AtomicBoolean callBackCalled = new AtomicBoolean(false);
		
		HashSet<AgentErrorCode> concernedAgentErrors = new HashSet<>();
		concernedAgentErrors.add(AgentErrorCode.UNEXPECTED);
		
		OutputMessage out = new OutputMessage();
		AgentError agentError = new AgentError(AgentErrorCode.CONTEXT_BUILDER);
		out.setAgentError(agentError);
		
		TokenLifecycleStrategy s = new ConfigurableTokenLifecycleStrategy(true, true, true, true, concernedAgentErrors);
		s.afterTokenCall(new TokenLifecycleStrategyCallback(null, null) {

			@Override
			public void addTokenError(String errorMessage, Exception exception) {
				Assert.assertEquals("my message", exception.getMessage());
				callBackCalled.set(true);
			}
			
		}, new TokenWrapper(), out);

		Assert.assertFalse(callBackCalled.get());
	}
	
	@Test
	public void testAddErrorOnAgentErrorTrue() {
		AtomicBoolean callBackCalled = new AtomicBoolean(false);
		
		HashSet<AgentErrorCode> concernedAgentErrors = new HashSet<>();
		concernedAgentErrors.add(AgentErrorCode.UNEXPECTED);
		
		OutputMessage out = new OutputMessage();
		AgentError agentError = new AgentError(AgentErrorCode.UNEXPECTED);
		out.setAgentError(agentError);
		
		TokenLifecycleStrategy s = new ConfigurableTokenLifecycleStrategy(true, true, true, true, concernedAgentErrors);
		s.afterTokenCall(new TokenLifecycleStrategyCallback(null, null) {

			@Override
			public void addTokenError(String errorMessage, Exception exception) {
				callBackCalled.set(true);
			}
			
		}, new TokenWrapper(), out);

		Assert.assertTrue(callBackCalled.get());
	}

}
