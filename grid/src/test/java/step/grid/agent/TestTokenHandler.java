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
package step.grid.agent;

import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class TestTokenHandler implements MessageHandler {

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		OutputMessage output = new OutputMessage();
		output.setPayload(message.getArgument());
		
		if(message.getArgument().containsKey("delay")) {
			Integer delay = message.getArgument().getInt("delay");
						
			boolean notInterruptable = message.getArgument().getBoolean("notInterruptable", false);
			if(notInterruptable) {
				sleepWithoutInterruptionUntil(System.currentTimeMillis()+delay);
			} else {
				sleep(delay);			
			}
		} else if(message.getArgument().containsKey("exception")) {
			throw new Exception(message.getArgument().getString("exception"));
		} else if(message.getArgument().containsKey("exceptionWithoutMessage")) {
			throw new Exception();
		} else if(message.getArgument().containsKey("error")) {
			output.setError(message.getArgument().getString("error"));
		} 
		
		return output;
	}

	private void sleep(Integer delay) throws InterruptedException {
		Thread.sleep(delay.longValue());
	}
	
	private void sleepWithoutInterruptionUntil(long until) {
		try {
			Thread.sleep(Math.max(0, until-System.currentTimeMillis()));
		} catch (InterruptedException e) {
			if(System.currentTimeMillis()<until) {
				sleepWithoutInterruptionUntil(until);				
			}
		}
	}

}
