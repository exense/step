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
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) {
		OutputMessage output = new OutputMessage();
		output.setPayload(message.getArgument());
		
		if(message.getArgument().containsKey("delay")) {
			Integer delay = message.getArgument().getInt("delay");
			
			long t1 = System.currentTimeMillis();
			
			boolean reSleepOnInterruption = message.getArgument().getBoolean("notInterruptable", false);
			sleep(t1, delay, reSleepOnInterruption);			
		}
		
		return output;
	}

	private void sleep(long t1, Integer delay, boolean reSleepOnInterruption) {
		long t = System.currentTimeMillis();
		try {
			Thread.sleep(t1+delay.longValue()-t);
		} catch (InterruptedException e) {
			if(reSleepOnInterruption && t<t1+delay) {
				sleep(t1, delay, true);				
			}
		}
	}

}
